package com.promptly.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Demande de generation d'un QCM a partir du titre, de la description et du cours. */
public record GenerateQuizRequest(

        @NotBlank @Size(min = 3, max = 150)
        String title,

        @NotBlank @Size(min = 10, max = 2000)
        String description,

        @NotNull
        Long courseId,

        Long chapterId
) {
}
