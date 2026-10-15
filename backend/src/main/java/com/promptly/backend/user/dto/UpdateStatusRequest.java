package com.promptly.backend.user.dto;

import jakarta.validation.constraints.NotNull;

/** Activation ou desactivation d'un compte, reserve aux administrateurs. */
public record UpdateStatusRequest(

        @NotNull(message = "Le statut est obligatoire")
        Boolean enabled
) {
}
