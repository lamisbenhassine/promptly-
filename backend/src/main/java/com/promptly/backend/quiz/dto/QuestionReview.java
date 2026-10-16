package com.promptly.backend.quiz.dto;

import java.util.List;

/**
 * Correction d'une question pour une tentative.
 * {@code integrityLabel} / {@code integrityConfidence} : visibles enseignant seulement
 * (GUESS = reponse au hasard, KNOW = semble connaitre).
 */
public record QuestionReview(
        Long questionId,
        String text,
        int position,
        boolean answeredCorrectly,
        List<ChoiceReview> choices,
        String integrityLabel,
        Double integrityConfidence,
        Long timeSpentMs
) {
}
