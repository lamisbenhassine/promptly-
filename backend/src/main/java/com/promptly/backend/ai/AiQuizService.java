package com.promptly.backend.ai;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptly.backend.ai.dto.GenerateQuizRequest;
import com.promptly.backend.ai.dto.GeneratedQuizResponse;
import com.promptly.backend.chapter.Chapter;
import com.promptly.backend.chapter.ChapterRepository;
import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.course.Course;
import com.promptly.backend.course.CourseService;
import com.promptly.backend.quiz.dto.ChoiceRequest;
import com.promptly.backend.quiz.dto.QuestionRequest;
import com.promptly.backend.storage.FileStorageService;

/**
 * Generation d'un QCM (10 questions) via Ollama a partir du titre, de la
 * description et du contenu du cours / chapitre / PDF.
 */
@Service
@Transactional(readOnly = true)
public class AiQuizService {

    private static final Logger log = LoggerFactory.getLogger(AiQuizService.class);
    private static final int QUESTION_COUNT = 10;
    private static final int CHOICES_PER_QUESTION = 4;
    private static final int CONTEXT_LIMIT = 8000;
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");

    private final CourseService courseService;
    private final ChapterRepository chapterRepository;
    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public AiQuizService(CourseService courseService,
                         ChapterRepository chapterRepository,
                         FileStorageService fileStorageService,
                         PdfTextExtractor pdfTextExtractor,
                         OllamaClient ollamaClient,
                         ObjectMapper objectMapper) {
        this.courseService = courseService;
        this.chapterRepository = chapterRepository;
        this.fileStorageService = fileStorageService;
        this.pdfTextExtractor = pdfTextExtractor;
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
    }

    public GeneratedQuizResponse generate(GenerateQuizRequest request, String editorEmail) {
        // Meme regle que la creation de quiz : enseignant du cours ou admin.
        Course course = courseService.requireManageable(request.courseId(), editorEmail);

        Chapter chapter = null;
        if (request.chapterId() != null) {
            chapter = chapterRepository.findByIdAndCourseId(request.chapterId(), course.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Chapitre", request.chapterId()));
        }

        String context = buildContext(course, chapter);
        String prompt = """
                Tu es un enseignant qui cree un QCM pour la plateforme Promptly.
                Genere EXACTEMENT 10 questions a choix multiples en francais.

                Consignes :
                - chaque question a EXACTEMENT 4 choix
                - EXACTEMENT 1 choix correct par question (correct=true), les 3 autres false
                - questions basees sur le titre, la description et le contenu fourni
                - niveau etudiant, claires, sans piege absurde
                - ne invente pas de faits hors contexte si le contexte est riche
                - reponds UNIQUEMENT en JSON valide

                Format exact :
                {"questions":[{"text":"enonce","choices":[{"text":"A","correct":true},{"text":"B","correct":false},{"text":"C","correct":false},{"text":"D","correct":false}]}]}

                Titre du quiz : %s
                Description du quiz : %s

                Contexte pedagogique :
                %s
                """.formatted(
                request.title().trim(),
                request.description().trim(),
                context);

        String raw = ollamaClient.generate(prompt, 0.2, 3500, true);
        log.debug("Quiz Ollama brut ({} chars)", raw.length());

        List<QuestionRequest> questions = parseQuestions(raw);
        if (questions.size() < QUESTION_COUNT) {
            // Second essai sans format json force, prompt plus court.
            String retryPrompt = """
                    Genere un QCM JSON de 10 questions, 4 choix, 1 seule bonne reponse.
                    Format: {"questions":[{"text":"...","choices":[{"text":"...","correct":true},{"text":"...","correct":false},{"text":"...","correct":false},{"text":"...","correct":false}]}]}
                    Titre: %s
                    Description: %s
                    Cours: %s
                    """.formatted(request.title(), request.description(), course.getTitle());
            questions = parseQuestions(ollamaClient.generate(retryPrompt, 0.15, 3500, true));
        }

        if (questions.isEmpty()) {
            throw new BusinessRuleException(
                    "Impossible de generer le quiz. Verifiez Ollama et reessayez.");
        }

        List<QuestionRequest> normalized = normalizeToTen(questions);
        return new GeneratedQuizResponse(normalized, ollamaClient.modelName(), course.getTitle());
    }

    private String buildContext(Course course, Chapter chapter) {
        StringBuilder sb = new StringBuilder();
        sb.append("Cours : ").append(course.getTitle()).append('\n');
        sb.append("Description du cours : ").append(course.getDescription()).append('\n');
        sb.append("Objectifs : ").append(course.getLearningObjectives()).append('\n');

        if (chapter != null) {
            sb.append("\nChapitre ").append(chapter.getPosition()).append(" : ")
                    .append(chapter.getTitle()).append('\n');
            sb.append(chapter.getContent()).append('\n');
            if (chapter.getCodeExample() != null && !chapter.getCodeExample().isBlank()) {
                sb.append("Exemple de code :\n").append(chapter.getCodeExample()).append('\n');
            }
            appendPdf(sb, chapter.getPdfPath(), "PDF du chapitre");
        } else {
            appendPdf(sb, course.getPdfPath(), "PDF du cours");
            List<Chapter> chapters = chapterRepository.findByCourseIdOrderByPositionAsc(course.getId());
            int remaining = CONTEXT_LIMIT - sb.length();
            for (Chapter ch : chapters) {
                if (remaining < 200) {
                    break;
                }
                String block = "\nChapitre " + ch.getPosition() + " - " + ch.getTitle() + " : "
                        + shorten(ch.getContent(), Math.min(800, remaining)) + "\n";
                sb.append(block);
                remaining = CONTEXT_LIMIT - sb.length();
            }
        }

        return shorten(sb.toString(), CONTEXT_LIMIT);
    }

    private void appendPdf(StringBuilder sb, String relativePath, String label) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path path = fileStorageService.resolveExisting(relativePath);
            String text = pdfTextExtractor.extract(path);
            sb.append('\n').append(label).append(" :\n")
                    .append(shorten(text, 4000)).append('\n');
        } catch (Exception e) {
            log.debug("PDF ignore pour generation quiz: {}", e.getMessage());
        }
    }

    private List<QuestionRequest> parseQuestions(String raw) {
        List<QuestionRequest> questions = new ArrayList<>();
        try {
            String json = extractJson(raw);
            JsonNode root = objectMapper.readTree(json);
            JsonNode array = root.has("questions") ? root.get("questions") : root;
            if (!array.isArray()) {
                return questions;
            }

            int position = 1;
            for (JsonNode qNode : array) {
                String text = firstText(qNode, "text", "question", "enonce");
                JsonNode choicesNode = qNode.get("choices");
                if (choicesNode == null) {
                    choicesNode = qNode.get("options");
                }
                if (text.isBlank() || choicesNode == null || !choicesNode.isArray()) {
                    continue;
                }

                List<ChoiceRequest> choices = new ArrayList<>();
                for (JsonNode cNode : choicesNode) {
                    String choiceText = firstText(cNode, "text", "label", "answer");
                    boolean correct = false;
                    if (cNode.has("correct")) {
                        correct = cNode.get("correct").asBoolean(false);
                    } else if (cNode.has("isCorrect")) {
                        correct = cNode.get("isCorrect").asBoolean(false);
                    }
                    if (!choiceText.isBlank()) {
                        choices.add(new ChoiceRequest(choiceText.trim(), correct));
                    }
                }

                choices = ensureValidChoices(choices);
                if (choices.size() >= 2) {
                    questions.add(new QuestionRequest(text.trim(), position++, choices));
                }
                if (questions.size() == QUESTION_COUNT) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Parse quiz Ollama echoue: {}", e.getMessage());
        }
        return questions;
    }

    private List<ChoiceRequest> ensureValidChoices(List<ChoiceRequest> input) {
        List<ChoiceRequest> choices = new ArrayList<>(input);
        while (choices.size() > CHOICES_PER_QUESTION) {
            // Prefer garder une bonne reponse.
            int removeIdx = -1;
            for (int i = choices.size() - 1; i >= 0; i--) {
                if (!choices.get(i).correct()) {
                    removeIdx = i;
                    break;
                }
            }
            if (removeIdx < 0) {
                removeIdx = choices.size() - 1;
            }
            choices.remove(removeIdx);
        }
        while (choices.size() < CHOICES_PER_QUESTION) {
            choices.add(new ChoiceRequest("Proposition " + (choices.size() + 1), false));
        }

        long correctCount = choices.stream().filter(ChoiceRequest::correct).count();
        if (correctCount == 0) {
            ChoiceRequest first = choices.get(0);
            choices.set(0, new ChoiceRequest(first.text(), true));
        } else if (correctCount > 1) {
            boolean kept = false;
            List<ChoiceRequest> fixed = new ArrayList<>();
            for (ChoiceRequest choice : choices) {
                if (choice.correct() && !kept) {
                    fixed.add(choice);
                    kept = true;
                } else {
                    fixed.add(new ChoiceRequest(choice.text(), false));
                }
            }
            choices = fixed;
        }
        return choices;
    }

    private List<QuestionRequest> normalizeToTen(List<QuestionRequest> source) {
        List<QuestionRequest> result = new ArrayList<>();
        for (int i = 0; i < source.size() && result.size() < QUESTION_COUNT; i++) {
            QuestionRequest q = source.get(i);
            result.add(new QuestionRequest(q.text(), result.size() + 1, ensureValidChoices(q.choices())));
        }
        // Si moins de 10, duplique en variant le numero (dernier recours).
        int guard = 0;
        while (result.size() < QUESTION_COUNT && !source.isEmpty() && guard++ < 20) {
            QuestionRequest base = source.get(result.size() % source.size());
            result.add(new QuestionRequest(
                    base.text() + " (variante " + (result.size() + 1) + ")",
                    result.size() + 1,
                    ensureValidChoices(base.choices())));
        }
        return result;
    }

    private static String extractJson(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        Matcher matcher = JSON_OBJECT.matcher(cleaned);
        String best = null;
        while (matcher.find()) {
            String candidate = matcher.group();
            if (candidate.contains("questions") || candidate.contains("choices")) {
                return candidate;
            }
            if (best == null) {
                best = candidate;
            }
        }
        return best != null ? best : cleaned;
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText("").trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= max ? cleaned : cleaned.substring(0, max);
    }
}
