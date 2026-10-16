package com.promptly.backend.quiz.dto;

import java.time.Instant;
import java.util.List;

import com.promptly.backend.quiz.Quiz;

public record QuizResponse(
        Long id,
        String title,
        String description,
        boolean published,
        Integer durationMinutes,
        Integer secondsPerQuestion,
        Long courseId,
        String courseTitle,
        Long chapterId,
        String chapterTitle,
        int questionCount,
        List<QuestionResponse> questions,
        Instant createdAt,
        Instant updatedAt
) {
    public static QuizResponse summary(Quiz quiz) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.isPublished(),
                quiz.getDurationMinutes(),
                quiz.getSecondsPerQuestion(),
                quiz.getCourse().getId(),
                quiz.getCourse().getTitle(),
                quiz.getChapter() != null ? quiz.getChapter().getId() : null,
                quiz.getChapter() != null ? quiz.getChapter().getTitle() : null,
                quiz.getQuestions().size(),
                null,
                quiz.getCreatedAt(),
                quiz.getUpdatedAt());
    }

    public static QuizResponse detail(Quiz quiz, boolean revealCorrect) {
        return new QuizResponse(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.isPublished(),
                quiz.getDurationMinutes(),
                quiz.getSecondsPerQuestion(),
                quiz.getCourse().getId(),
                quiz.getCourse().getTitle(),
                quiz.getChapter() != null ? quiz.getChapter().getId() : null,
                quiz.getChapter() != null ? quiz.getChapter().getTitle() : null,
                quiz.getQuestions().size(),
                quiz.getQuestions().stream()
                        .map(q -> QuestionResponse.from(q, revealCorrect))
                        .toList(),
                quiz.getCreatedAt(),
                quiz.getUpdatedAt());
    }
}
