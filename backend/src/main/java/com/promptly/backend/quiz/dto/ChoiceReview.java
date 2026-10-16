package com.promptly.backend.quiz.dto;

/** Choix dans le detail d'une tentative (bonne reponse + selection etudiant). */
public record ChoiceReview(
        Long id,
        String text,
        boolean correct,
        boolean selected
) {
}
