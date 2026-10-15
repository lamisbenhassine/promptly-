package com.promptly.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Modification du profil personnel.
 *
 * Le courriel n'est pas modifiable : il sert d'identifiant de connexion et son
 * changement exigerait une procedure de verification. Le role et le statut ne
 * figurent pas non plus dans ce DTO, afin qu'un utilisateur ne puisse pas
 * s'attribuer lui-meme des droits en modifiant son profil.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Le prenom est obligatoire")
        @Size(min = 2, max = 50, message = "Le prenom doit contenir entre 2 et 50 caracteres")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caracteres")
        String lastName
) {
}
