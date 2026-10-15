package com.promptly.backend.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de verification de l'etat du backend.
 *
 * Il sert a confirmer, depuis Postman ou depuis Angular, que l'API repond et
 * que la connexion a la base de donnees MySQL est operationnelle.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final int VALIDATION_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;
    private final String applicationName;
    private final String applicationVersion;

    public HealthController(DataSource dataSource,
                            @Value("${spring.application.name}") String applicationName,
                            @Value("${promptly.application.version}") String applicationVersion) {
        this.dataSource = dataSource;
        this.applicationName = applicationName;
        this.applicationVersion = applicationVersion;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                applicationName,
                applicationVersion,
                checkDatabase(),
                Instant.now()
        );
    }

    /**
     * Ouvre une connexion JDBC et verifie qu'elle est valide.
     * L'API reste disponible meme si la base est injoignable : seul le champ
     * "database" passe a DOWN, ce qui facilite le diagnostic.
     */
    private String checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(VALIDATION_TIMEOUT_SECONDS) ? "UP" : "DOWN";
        } catch (SQLException e) {
            return "DOWN: " + e.getMessage();
        }
    }
}
