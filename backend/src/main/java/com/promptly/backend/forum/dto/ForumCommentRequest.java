package com.promptly.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumCommentRequest(
        @NotBlank @Size(min = 2, max = 5000) String content
) {
}
