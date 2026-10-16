package com.promptly.backend.ai.dto;

/** Resume genere par Ollama a partir d'un PDF. */
public record SummaryResponse(
        String summary,
        String model,
        String sourceTitle,
        boolean truncatedHint
) {
}
