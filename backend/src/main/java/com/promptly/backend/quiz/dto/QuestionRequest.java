package com.promptly.backend.quiz.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record QuestionRequest(
        @NotBlank @Size(max = 1000) String text,
        Integer position,
        @NotEmpty @Valid List<ChoiceRequest> choices
) {
}
