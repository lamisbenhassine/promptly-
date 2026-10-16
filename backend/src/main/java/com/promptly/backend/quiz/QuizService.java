package com.promptly.backend.quiz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.promptly.backend.chapter.Chapter;
import com.promptly.backend.chapter.ChapterRepository;
import com.promptly.backend.common.exception.BusinessRuleException;
import com.promptly.backend.common.exception.ResourceNotFoundException;
import com.promptly.backend.course.Course;
import com.promptly.backend.course.CourseService;
import com.promptly.backend.quiz.dto.AttemptRequest;
import com.promptly.backend.quiz.dto.AttemptResponse;
import com.promptly.backend.quiz.dto.ChoiceRequest;
import com.promptly.backend.quiz.dto.QuestionRequest;
import com.promptly.backend.quiz.dto.QuizRequest;
import com.promptly.backend.quiz.dto.QuizResponse;
import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;
import com.promptly.backend.user.UserService;

@Service
@Transactional(readOnly = true)
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final ChapterRepository chapterRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public QuizService(QuizRepository quizRepository,
                       QuizAttemptRepository attemptRepository,
                       ChapterRepository chapterRepository,
                       CourseService courseService,
                       UserService userService,
                       ObjectMapper objectMapper) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.chapterRepository = chapterRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    public List<QuizResponse> list(String viewerEmail, Long courseId) {
        User viewer = userService.findByEmail(viewerEmail);
        boolean seeAll = viewer.getRole() == Role.ADMIN;
        return quizRepository.search(courseId, seeAll, viewer).stream()
                .map(QuizResponse::summary)
                .toList();
    }

    public QuizResponse get(Long id, String viewerEmail) {
        User viewer = userService.findByEmail(viewerEmail);
        Quiz quiz = requireVisible(id, viewer);
        boolean reveal = canManage(quiz, viewer);
        return QuizResponse.detail(quiz, reveal);
    }

    @Transactional
    public QuizResponse create(String editorEmail, QuizRequest request) {
        Course course = courseService.requireManageable(request.courseId(), editorEmail);
        Chapter chapter = resolveChapter(request.chapterId(), course);

        Quiz quiz = new Quiz(
                request.title().trim(),
                request.description().trim(),
                request.published(),
                course,
                chapter);

        quiz.replaceQuestions(buildQuestions(request.questions(), request.published()));
        return QuizResponse.detail(quizRepository.save(quiz), true);
    }

    @Transactional
    public QuizResponse update(Long id, String editorEmail, QuizRequest request) {
        User editor = userService.findByEmail(editorEmail);
        Quiz quiz = findWithDetails(id);
        ensureCanManage(quiz, editor);

        Course course = courseService.requireManageable(request.courseId(), editorEmail);
        Chapter chapter = resolveChapter(request.chapterId(), course);

        quiz.setTitle(request.title().trim());
        quiz.setDescription(request.description().trim());
        quiz.setPublished(request.published());
        quiz.setCourse(course);
        quiz.setChapter(chapter);
        quiz.replaceQuestions(buildQuestions(request.questions(), request.published()));

        return QuizResponse.detail(quizRepository.save(quiz), true);
    }

    @Transactional
    public void delete(Long id, String editorEmail) {
        User editor = userService.findByEmail(editorEmail);
        Quiz quiz = findWithDetails(id);
        ensureCanManage(quiz, editor);
        attemptRepository.findByQuizIdOrderBySubmittedAtDesc(id)
                .forEach(attemptRepository::delete);
        quizRepository.delete(quiz);
    }

    @Transactional
    public AttemptResponse submit(Long quizId, String studentEmail, AttemptRequest request) {
        User student = userService.findByEmail(studentEmail);
        Quiz quiz = requireVisible(quizId, student);

        if (!quiz.isPublished() || !quiz.getCourse().isPublished()) {
            throw new BusinessRuleException("Ce quiz n'est pas encore ouvert aux tentatives.");
        }
        if (quiz.getQuestions().isEmpty()) {
            throw new BusinessRuleException("Ce quiz ne contient aucune question.");
        }

        Set<Long> selected = new HashSet<>(request.selectedChoiceIds());
        int correctCount = 0;

        for (Question question : quiz.getQuestions()) {
            Set<Long> correctIds = new HashSet<>();
            Set<Long> questionChoiceIds = new HashSet<>();
            for (Choice choice : question.getChoices()) {
                questionChoiceIds.add(choice.getId());
                if (choice.isCorrect()) {
                    correctIds.add(choice.getId());
                }
            }

            Set<Long> selectedForQuestion = new HashSet<>();
            for (Long choiceId : selected) {
                if (questionChoiceIds.contains(choiceId)) {
                    selectedForQuestion.add(choiceId);
                }
            }

            if (selectedForQuestion.equals(correctIds) && !correctIds.isEmpty()) {
                correctCount++;
            }
        }

        int total = quiz.getQuestions().size();
        int scorePercent = total == 0 ? 0 : (int) Math.round((correctCount * 100.0) / total);

        String json;
        try {
            json = objectMapper.writeValueAsString(request.selectedChoiceIds());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossible de serialiser les reponses", ex);
        }

        QuizAttempt attempt = attemptRepository.save(new QuizAttempt(
                quiz, student, scorePercent, correctCount, total, json));

        return AttemptResponse.from(attempt, request.selectedChoiceIds());
    }

    public List<AttemptResponse> myAttempts(Long quizId, String viewerEmail) {
        User viewer = userService.findByEmail(viewerEmail);
        requireVisible(quizId, viewer);
        return attemptRepository
                .findByQuizIdAndStudentIdOrderBySubmittedAtDesc(quizId, viewer.getId())
                .stream()
                .map(this::toAttemptResponse)
                .toList();
    }

    public List<AttemptResponse> allAttempts(Long quizId, String editorEmail) {
        User editor = userService.findByEmail(editorEmail);
        Quiz quiz = findWithDetails(quizId);
        ensureCanManage(quiz, editor);
        return attemptRepository.findByQuizIdOrderBySubmittedAtDesc(quizId).stream()
                .map(this::toAttemptResponse)
                .toList();
    }

    private AttemptResponse toAttemptResponse(QuizAttempt attempt) {
        List<Long> selected;
        try {
            selected = objectMapper.readValue(
                    attempt.getSelectedChoiceIdsJson(),
                    new TypeReference<List<Long>>() { });
        } catch (JsonProcessingException ex) {
            selected = List.of();
        }
        return AttemptResponse.from(attempt, selected);
    }

    private List<Question> buildQuestions(List<QuestionRequest> requests, boolean published) {
        List<QuestionRequest> items = requests == null ? List.of() : requests;
        if (published && items.isEmpty()) {
            throw new BusinessRuleException("Un quiz publie doit contenir au moins une question.");
        }

        List<Question> questions = new ArrayList<>();
        int index = 1;
        for (QuestionRequest request : items) {
            validateQuestion(request);
            int position = request.position() != null ? request.position() : index;
            Question question = new Question(request.text().trim(), position);
            List<Choice> choices = request.choices().stream()
                    .map(c -> new Choice(c.text().trim(), c.correct()))
                    .toList();
            question.replaceChoices(new ArrayList<>(choices));
            questions.add(question);
            index++;
        }
        return questions;
    }

    private void validateQuestion(QuestionRequest request) {
        if (request.choices() == null || request.choices().size() < 2) {
            throw new BusinessRuleException("Chaque question doit avoir au moins deux choix.");
        }
        boolean hasCorrect = request.choices().stream().anyMatch(ChoiceRequest::correct);
        if (!hasCorrect) {
            throw new BusinessRuleException("Chaque question doit avoir au moins une bonne reponse.");
        }
    }

    private Chapter resolveChapter(Long chapterId, Course course) {
        if (chapterId == null) {
            return null;
        }
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("Chapitre", chapterId));
        if (!chapter.getCourse().getId().equals(course.getId())) {
            throw new BusinessRuleException("Le chapitre n'appartient pas au cours selectionne.");
        }
        return chapter;
    }

    private Quiz requireVisible(Long id, User viewer) {
        Quiz quiz = findWithDetails(id);
        if (!isVisible(quiz, viewer)) {
            throw new ResourceNotFoundException("Quiz", id);
        }
        return quiz;
    }

    private boolean isVisible(Quiz quiz, User viewer) {
        if (viewer.getRole() == Role.ADMIN) {
            return true;
        }
        if (canManage(quiz, viewer)) {
            return true;
        }
        return quiz.isPublished() && quiz.getCourse().isPublished();
    }

    private boolean canManage(Quiz quiz, User user) {
        return user.getRole() == Role.ADMIN
                || quiz.getCourse().getInstructor().getId().equals(user.getId());
    }

    private void ensureCanManage(Quiz quiz, User user) {
        if (!canManage(quiz, user)) {
            throw new AccessDeniedException("Vous ne pouvez pas modifier ce quiz.");
        }
    }

    private Quiz findWithDetails(Long id) {
        return quizRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
    }
}
