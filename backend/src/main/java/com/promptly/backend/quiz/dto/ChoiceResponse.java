package com.promptly.backend.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.promptly.backend.quiz.Choice;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChoiceResponse(
        Long id,
        String text,
        Boolean correct
) {
    public static ChoiceResponse from(Choice choice, boolean revealCorrect) {
        return new ChoiceResponse(
                choice.getId(),
                choice.getText(),
                revealCorrect ? choice.isCorrect() : null);
    }
}
