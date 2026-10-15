package com.promptly.backend.auth.dto;

import java.time.Instant;

import com.promptly.backend.user.dto.UserResponse;

/**
 * Reponse renvoyee apres une connexion reussie.
 *
 * @param token     jeton JWT a placer dans l'en-tete Authorization
 * @param tokenType schema d'authentification ("Bearer")
 * @param expiresAt date d'expiration du jeton
 * @param user      profil de l'utilisateur connecte
 */
public record AuthResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserResponse user
) {

    public static AuthResponse of(String token, Instant expiresAt, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresAt, user);
    }
}
