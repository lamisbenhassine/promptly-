package com.promptly.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Donnees d'inscription.
 *
 * Le role n'est volontairement pas demande : tout nouveau compte est cree avec
 * le role ETUDIANT. Si le client pouvait transmettre un role, n'importe qui
 * s'inscrirait comme administrateur. La promotion vers ENSEIGNANT ou ADMIN est
 * reservee a un administrateur deja en place.
 */
public record RegisterRequest(

        @NotBlank(message = "Le prenom est obligatoire")
        @Size(min = 2, max = 50, message = "Le prenom doit contenir entre 2 et 50 caracteres")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caracteres")
        String lastName,

        @NotBlank(message = "L'adresse electronique est obligatoire")
        @Email(message = "L'adresse electronique n'est pas valide")
        @Size(max = 180, message = "L'adresse electronique est trop longue")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir au moins 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$",
                message = "Le mot de passe doit contenir au moins une lettre et un chiffre"
        )
        String password
) {
}
