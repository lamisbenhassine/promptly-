package com.promptly.backend.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Depot utilisateur : objet + description uniquement.
 * Type / urgence / priorite sont predits par le ML cote serveur.
 */
public record ComplaintRequest(
        @NotBlank @Size(min = 5, max = 180) String subject,
        @NotBlank @Size(min = 10, max = 5000) String description
) {
}
