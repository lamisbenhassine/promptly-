package com.promptly.backend.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChoiceRequest(
        @NotBlank @Size(max = 500) String text,
        boolean correct
) {
}
