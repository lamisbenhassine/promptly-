package com.promptly.backend.common;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Format unique des reponses d'erreur de l'API.
 *
 * Disposer d'une structure unique permet au frontend Angular de traiter toutes
 * les erreurs de la meme maniere, quel que soit l'endpoint appele.
 *
 * @param status      code HTTP
 * @param error       libelle du code HTTP
 * @param message     message destine a l'utilisateur
 * @param path        URL appelee
 * @param fieldErrors erreurs de validation par champ (absent si aucune)
 * @param timestamp   instant de l'erreur
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        Instant timestamp
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, null, Instant.now());
    }

    public static ApiError validation(String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(400, "Bad Request", message, path, fieldErrors, Instant.now());
    }
}
