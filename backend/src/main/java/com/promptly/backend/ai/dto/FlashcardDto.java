package com.promptly.backend.ai.dto;

/** Une carte : face (titre / question) et dos (astuce). */
public record FlashcardDto(String front, String back) {
}
