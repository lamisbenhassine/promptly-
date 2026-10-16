package com.promptly.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Corps de POST /api/auth/google : jeton d'identite emis par Google. */
public record GoogleLoginRequest(

        @NotBlank(message = "Le jeton Google est obligatoire")
        String idToken
) {
}
