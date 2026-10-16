package com.promptly.backend.quiz.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuizRequest(
        @NotBlank @Size(min = 3, max = 150) String title,
        @NotBlank @Size(min = 10, max = 2000) String description,
        @NotNull Long courseId,
        Long chapterId,
        boolean published,
        @Min(1) @Max(180) Integer durationMinutes,
        @Min(10) @Max(600) Integer secondsPerQuestion,
        @Valid List<QuestionRequest> questions
) {
}
