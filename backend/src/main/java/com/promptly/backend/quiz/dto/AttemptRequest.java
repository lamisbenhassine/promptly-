package com.promptly.backend.quiz.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record AttemptRequest(
        @NotEmpty List<Long> selectedChoiceIds
) {
}
