package com.promptly.backend.user.dto;

import java.time.Instant;

import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;

/**
 * Representation d'un utilisateur renvoyee par l'API.
 *
 * Ce DTO est distinct de l'entite : le champ {@code password} n'y figure pas,
 * ce qui rend structurellement impossible la divulgation du mot de passe hache,
 * meme par erreur.
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
