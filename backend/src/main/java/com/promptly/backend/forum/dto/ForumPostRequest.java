package com.promptly.backend.forum.dto;

import com.promptly.backend.forum.ForumCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ForumPostRequest(
        @NotBlank @Size(min = 5, max = 180) String title,
        @NotBlank @Size(min = 10, max = 10000) String content,
        @NotNull ForumCategory category
) {
}
