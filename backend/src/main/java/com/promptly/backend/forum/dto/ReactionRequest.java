package com.promptly.backend.forum.dto;

import com.promptly.backend.forum.ReactionType;

import jakarta.validation.constraints.NotNull;

public record ReactionRequest(@NotNull ReactionType type) {
}
