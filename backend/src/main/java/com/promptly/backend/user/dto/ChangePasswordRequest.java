package com.promptly.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Changement de mot de passe.
 *
 * L'ancien mot de passe est exige : sans cette verification, un attaquant ayant
 * obtenu un jeton valide pourrait prendre definitivement le controle du compte.
 */
public record ChangePasswordRequest(

        @NotBlank(message = "Le mot de passe actuel est obligatoire")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir au moins 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir au moins une lettre et un chiffre"
        )
        String newPassword
) {
}
