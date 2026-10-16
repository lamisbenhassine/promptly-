package com.promptly.backend.ai.dto;

import java.util.List;

/** Quatre flashcards generees a partir d'un PDF. */
public record FlashcardsResponse(
        List<FlashcardDto> cards,
        String model,
        String sourceTitle
) {
}
