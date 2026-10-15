package com.promptly.backend.health;

import java.time.Instant;

/**
 * Reponse renvoyee par l'endpoint de verification /api/health.
 *
 * Un "record" Java 17 est utilise car il s'agit d'un simple porteur de donnees
 * immuable : le compilateur genere le constructeur, les accesseurs et equals().
 *
 * @param status      etat global de l'API ("UP")
 * @param application nom de l'application
 * @param version     version de l'application
 * @param database    etat de la connexion MySQL ("UP", "DOWN" ou message d'erreur)
 * @param timestamp   instant de la verification
 */
public record HealthResponse(
        String status,
        String application,
        String version,
        String database,
        Instant timestamp
) {
}
