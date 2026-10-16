package com.promptly.backend.ai.dto;

import java.util.List;

import com.promptly.backend.quiz.dto.QuestionRequest;

/** QCM genere par Ollama (a injecter dans le formulaire). */
public record GeneratedQuizResponse(
        List<QuestionRequest> questions,
        String model,
        String courseTitle
) {
}
