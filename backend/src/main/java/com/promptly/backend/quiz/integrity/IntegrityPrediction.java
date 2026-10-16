package com.promptly.backend.quiz.integrity;

/** Prediction GUESS / KNOW pour une question. */
public record IntegrityPrediction(
        long questionId,
        String label,
        double confidence
) {
}
