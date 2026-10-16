package com.promptly.backend.ai.dto;

/** Reponse de l'assistant (Ollama). */
public record ChatResponse(String reply, String model) {
}
