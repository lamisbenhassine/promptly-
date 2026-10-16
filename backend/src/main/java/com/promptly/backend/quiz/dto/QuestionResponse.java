package com.promptly.backend.quiz.dto;

import java.util.List;

import com.promptly.backend.quiz.Question;

public record QuestionResponse(
        Long id,
        String text,
        int position,
        List<ChoiceResponse> choices
) {
    public static QuestionResponse from(Question question, boolean revealCorrect) {
        return new QuestionResponse(
                question.getId(),
                question.getText(),
                question.getPosition(),
                question.getChoices().stream()
                        .map(choice -> ChoiceResponse.from(choice, revealCorrect))
                        .toList());
    }
}
