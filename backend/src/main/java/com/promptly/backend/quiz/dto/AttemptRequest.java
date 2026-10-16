package com.promptly.backend.quiz.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AttemptRequest(
        @NotNull List<Long> selectedChoiceIds,
        @Valid List<QuestionTimingRequest> questionTimings,
        /** true si le chrono global a expire (soumission automatique). */
        boolean timedOut
) {
}
