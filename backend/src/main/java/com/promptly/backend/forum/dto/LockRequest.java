package com.promptly.backend.forum.dto;

import jakarta.validation.constraints.NotNull;

public record LockRequest(@NotNull Boolean locked) {
}
