package com.promptly.backend.quiz.dto;

import java.time.Instant;
import java.util.List;

import com.promptly.backend.quiz.QuizAttempt;

public record AttemptResponse(
        Long id,
        Long quizId,
        String quizTitle,
        Long studentId,
        String studentEmail,
        String studentFirstName,
        String studentLastName,
        int scorePercent,
        int correctCount,
        int totalQuestions,
        List<Long> selectedChoiceIds,
        List<QuestionReview> review,
        Instant submittedAt
) {
    public static AttemptResponse from(QuizAttempt attempt,
                                       List<Long> selectedChoiceIds,
                                       List<QuestionReview> review) {
        return new AttemptResponse(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.getQuiz().getTitle(),
                attempt.getStudent().getId(),
                attempt.getStudent().getEmail(),
                attempt.getStudent().getFirstName(),
                attempt.getStudent().getLastName(),
                attempt.getScorePercent(),
                attempt.getCorrectCount(),
                attempt.getTotalQuestions(),
                selectedChoiceIds,
                review,
                attempt.getSubmittedAt());
    }
}
