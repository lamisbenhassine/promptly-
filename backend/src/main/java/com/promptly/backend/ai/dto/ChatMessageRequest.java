package com.promptly.backend.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Un tour de conversation (user ou assistant). */
public record ChatMessageRequest(

        @NotBlank
        @Pattern(regexp = "user|assistant", message = "role doit etre user ou assistant")
        String role,

        @NotBlank
        @Size(min = 1, max = 4000)
        String content
) {
}
