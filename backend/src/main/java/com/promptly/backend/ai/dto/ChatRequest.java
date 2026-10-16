package com.promptly.backend.ai.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Historique recent + dernier message pour l'assistant Promptly. */
public record ChatRequest(

        @NotEmpty
        @Size(max = 24)
        List<@Valid ChatMessageRequest> messages
) {
}
