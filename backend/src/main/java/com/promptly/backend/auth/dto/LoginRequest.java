package com.promptly.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Identifiants de connexion.
 *
 * Aucune contrainte de format n'est appliquee au mot de passe : les regles de
 * complexite valent a la creation, pas a la verification. Les imposer ici
 * reviendrait a renseigner un attaquant sur la politique de mots de passe.
 */
public record LoginRequest(

        @NotBlank(message = "L'adresse electronique est obligatoire")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String password
) {
}
