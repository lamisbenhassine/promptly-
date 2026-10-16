package com.promptly.backend.quiz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Chronometrage / comportement pour une question (envoye par le frontend). */
public record QuestionTimingRequest(
        @NotNull Long questionId,
        @Min(0) long timeSpentMs,
        @Min(0) int selectionChanges,
        @Min(0) int choiceIndex
) {
}
