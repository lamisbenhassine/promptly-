package com.promptly.backend.quiz.integrity;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Features envoyees au service ML d'integrite. */
public record IntegrityFeature(
        long questionId,
        long timeSpentMs,
        int selectionChanges,
        int choiceIndex,
        @JsonProperty("isCorrect") boolean isCorrect,
        double avgTimeMs
) {
}
