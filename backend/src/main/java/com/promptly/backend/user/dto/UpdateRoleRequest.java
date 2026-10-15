package com.promptly.backend.user.dto;

import com.promptly.backend.user.Role;

import jakarta.validation.constraints.NotNull;

/** Changement du role d'un utilisateur, reserve aux administrateurs. */
public record UpdateRoleRequest(

        @NotNull(message = "Le role est obligatoire")
        Role role
) {
}
