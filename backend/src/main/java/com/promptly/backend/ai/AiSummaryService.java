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
import com.promptly.backend.ai.dto.FlashcardDto;
import com.promptly.backend.ai.dto.FlashcardsResponse;
import com.promptly.backend.ai.dto.SummaryResponse;
import com.promptly.backend.chapter.Chapter;
import com.promptly.backend.chapter.ChapterRepository;
import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.course.Course;
import com.promptly.backend.course.CourseService;
import com.promptly.backend.storage.FileStorageService;

/**
 * Resume et flashcards automatiques des PDF via Ollama.
 */
@Service
@Transactional(readOnly = true)
public class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");
    private static final Pattern LINE_CARD = Pattern.compile(
            "(?im)^(?:CARD|CARTE)\\s*[|:]\\s*(.+?)\\s*[|:]\\s*(.+)$");
    private static final int FLASHCARD_TEXT_LIMIT = 6000;

    private final CourseService courseService;
    private final ChapterRepository chapterRepository;
    private final FileStorageService fileStorageService;
    private final PdfTextExtractor pdfTextExtractor;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    public AiSummaryService(CourseService courseService,
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

    public SummaryResponse summarizeCoursePdf(Long courseId, String viewerEmail) {
        PdfContext ctx = loadCoursePdf(courseId, viewerEmail);
        return summarize(ctx);
    }

    public SummaryResponse summarizeChapterPdf(Long courseId, Long chapterId, String viewerEmail) {
        PdfContext ctx = loadChapterPdf(courseId, chapterId, viewerEmail);
        return summarize(ctx);
    }

    public FlashcardsResponse flashcardsForCoursePdf(Long courseId, String viewerEmail) {
        return flashcards(loadCoursePdf(courseId, viewerEmail));
    }

    public FlashcardsResponse flashcardsForChapterPdf(Long courseId, Long chapterId, String viewerEmail) {
        return flashcards(loadChapterPdf(courseId, chapterId, viewerEmail));
    }

    private PdfContext loadCoursePdf(Long courseId, String viewerEmail) {
        Course course = courseService.requireVisible(courseId, viewerEmail);
        String pdfPath = course.getPdfPath();
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new BusinessRuleException("Ce cours n'a pas de PDF");
        }
        return new PdfContext(pdfPath, course.getTitle());
    }

    private PdfContext loadChapterPdf(Long courseId, Long chapterId, String viewerEmail) {
        courseService.requireVisible(courseId, viewerEmail);
        Chapter chapter = chapterRepository.findByIdAndCourseId(chapterId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre", chapterId));
        String pdfPath = chapter.getPdfPath();
        if (pdfPath == null || pdfPath.isBlank()) {
            throw new BusinessRuleException("Ce chapitre n'a pas de PDF");
        }
        return new PdfContext(pdfPath, chapter.getTitle());
    }

    private SummaryResponse summarize(PdfContext ctx) {
        Path absolute = fileStorageService.resolveExisting(ctx.relativePath());
        String text = pdfTextExtractor.extract(absolute);
        boolean truncated = text.contains("[Texte tronque pour le resume automatique]");

        String prompt = """
                Tu es un assistant pedagogique pour la plateforme e-learning Promptly.
                Resume le document suivant en francais, de facon claire et structuree.
                Consigne :
                - 5 a 10 phrases ou puces maximum
                - garde les idees essentielles et termes techniques importants
                - ne invente rien qui n'est pas dans le texte
                - commence directement par le resume, sans introduction

                Titre du document : %s

                Contenu :
                %s
                """.formatted(ctx.title(), text);

        String summary = ollamaClient.generate(prompt);
        return new SummaryResponse(summary, ollamaClient.modelName(), ctx.title(), truncated);
    }

    private FlashcardsResponse flashcards(PdfContext ctx) {
        Path absolute = fileStorageService.resolveExisting(ctx.relativePath());
        String text = shorten(pdfTextExtractor.extract(absolute), FLASHCARD_TEXT_LIMIT);

        String prompt = """
                Cree exactement 4 flashcards pedagogiques en francais a partir du document.
                Chaque carte = une idee cle / tip du cours.
                Ne invente rien hors du texte.

                Reponds avec un objet JSON de cette forme exacte :
                {"cards":[{"front":"titre court","back":"astuce en 1 a 3 phrases"},{"front":"...","back":"..."},{"front":"...","back":"..."},{"front":"...","back":"..."}]}

                Titre du document : %s

                Texte :
                %s
                """.formatted(ctx.title(), text);

        String raw = ollamaClient.generate(prompt, 0.1, 1200, true);
        log.debug("Flashcards brutes Ollama ({} chars): {}", raw.length(),
                raw.length() > 500 ? raw.substring(0, 500) + "..." : raw);

        List<FlashcardDto> cards = parseFlashcards(raw);
        if (cards.size() < 4) {
            // Second essai avec un format ligne simple (plus fiable sur petits modeles).
            String linePrompt = """
                    Donne EXACTEMENT 4 lignes, une par flashcard, au format :
                    CARD|titre court|astuce en une ou deux phrases
                    Pas d'autre texte. Titre: %s

                    Texte:
                    %s
                    """.formatted(ctx.title(), text);
            String lineRaw = ollamaClient.generate(linePrompt, 0.1, 800, false);
            log.debug("Flashcards lignes Ollama: {}", lineRaw);
            cards = parseFlashcards(lineRaw);
        }

        if (cards.isEmpty()) {
            throw new BusinessRuleException(
                    "Impossible d'interpreter les flashcards. Reessayez.");
        }

        while (cards.size() < 4) {
            FlashcardDto last = cards.get(cards.size() - 1);
            cards.add(new FlashcardDto(
                    "Astuce " + (cards.size() + 1),
                    last.back()));
        }

        return new FlashcardsResponse(cards.subList(0, 4), ollamaClient.modelName(), ctx.title());
    }

    private List<FlashcardDto> parseFlashcards(String raw) {
        List<FlashcardDto> fromJson = parseJsonCards(raw);
        if (!fromJson.isEmpty()) {
            return fromJson;
        }
        return parseLineCards(raw);
    }

    private List<FlashcardDto> parseJsonCards(String raw) {
        List<FlashcardDto> cards = new ArrayList<>();
        try {
            String json = extractJson(raw);
            JsonNode root = objectMapper.readTree(json);
            JsonNode cardsNode = findCardsArray(root);
            if (cardsNode == null || !cardsNode.isArray()) {
                return cards;
            }

            for (JsonNode node : cardsNode) {
                String front = firstNonBlank(node,
                        "front", "recto", "title", "titre", "question", "q");
                String back = firstNonBlank(node,
                        "back", "verso", "tip", "astuce", "answer", "reponse", "a", "content");
                if (!front.isBlank() && !back.isBlank()) {
                    cards.add(new FlashcardDto(front.trim(), back.trim()));
                }
                if (cards.size() == 4) {
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Parse JSON flashcards echoue: {}", e.getMessage());
        }
        return cards;
    }

    private List<FlashcardDto> parseLineCards(String raw) {
        List<FlashcardDto> cards = new ArrayList<>();
        Matcher matcher = LINE_CARD.matcher(raw);
        while (matcher.find() && cards.size() < 4) {
            String front = matcher.group(1).trim();
            String back = matcher.group(2).trim();
            if (!front.isBlank() && !back.isBlank()) {
                cards.add(new FlashcardDto(front, back));
            }
        }

        if (cards.isEmpty()) {
            // Fallback : lignes "1. titre - tip" ou "1) titre : tip"
            Pattern numbered = Pattern.compile(
                    "(?m)^\\s*\\d+[.)]\\s*(.+?)\\s*[-–:]\\s*(.+)$");
            Matcher numberedMatcher = numbered.matcher(raw);
            while (numberedMatcher.find() && cards.size() < 4) {
                cards.add(new FlashcardDto(
                        numberedMatcher.group(1).trim(),
                        numberedMatcher.group(2).trim()));
            }
        }
        return cards;
    }

    private static JsonNode findCardsArray(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        for (String key : List.of("cards", "flashcards", "cartes", "items", "data")) {
            if (root.has(key) && root.get(key).isArray()) {
                return root.get(key);
            }
        }
        return null;
    }

    private static String firstNonBlank(JsonNode node, String... fields) {
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

    private static String extractJson(String raw) {
        String cleaned = raw.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }
        // Prefere le plus petit objet JSON contenant "cards" / "front"
        Matcher matcher = JSON_OBJECT.matcher(cleaned);
        String best = null;
        while (matcher.find()) {
            String candidate = matcher.group();
            if (candidate.contains("front") || candidate.contains("cards")
                    || candidate.contains("titre") || candidate.contains("recto")) {
                best = candidate;
                break;
            }
            if (best == null) {
                best = candidate;
            }
        }
        return best != null ? best : cleaned;
    }

    private static String shorten(String text, int max) {
        String cleaned = text.replace("[Texte tronque pour le resume automatique]", "").trim();
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, max);
    }

    private record PdfContext(String relativePath, String title) {
    }
}
