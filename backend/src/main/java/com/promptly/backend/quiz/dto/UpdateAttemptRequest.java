package com.promptly.backend.quiz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Modification manuelle du resultat d'une tentative (enseignant / admin). */
public record UpdateAttemptRequest(

        @Min(0) @Max(100)
        int scorePercent,

        @Min(0)
        int correctCount
) {
}
