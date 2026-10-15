package com.promptly.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.promptly.backend.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Generation et verification des jetons JWT.
 *
 * Un jeton JWT comporte trois parties : un en-tete, une charge utile (les
 * "revendications") et une signature. La charge utile n'est pas chiffree, elle
 * est seulement encodee en Base64 : elle ne doit donc contenir aucune donnee
 * confidentielle. La signature, calculee avec une cle secrete connue du seul
 * serveur, garantit que le contenu n'a pas ete modifie par le client.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Longueur minimale de la cle imposee par l'algorithme HMAC-SHA256. */
    private static final int MINIMUM_KEY_LENGTH_BYTES = 32;

    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(@Value("${promptly.jwt.secret}") String secret,
                      @Value("${promptly.jwt.expiration-ms}") long expirationMillis) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // Verification au demarrage plutot qu'au premier appel : une application
        // mal configuree doit echouer immediatement, avec un message explicite.
        if (keyBytes.length < MINIMUM_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "La variable d'environnement PROMPTLY_JWT_SECRET doit contenir au moins "
                            + MINIMUM_KEY_LENGTH_BYTES + " caracteres (valeur actuelle : "
                            + keyBytes.length + ").");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMillis = expirationMillis;
    }

    /** Date d'expiration d'un jeton emis maintenant. */
    public Instant expirationFromNow() {
        return Instant.now().plusMillis(expirationMillis);
    }

    /**
     * Emet un jeton signe pour l'utilisateur donne.
     *
     * Le role est inclus a titre informatif pour le frontend (affichage des
     * menus). L'autorisation reelle est toujours recalculee cote serveur a
     * partir de la base, afin qu'une modification de role prenne effet sans
     * attendre l'expiration du jeton.
     */
    public String generateToken(User user, Instant expiresAt) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Verifie la signature et l'expiration du jeton, puis renvoie l'adresse
     * electronique qu'il contient.
     *
     * @return l'adresse electronique, ou {@code null} si le jeton est invalide
     */
    public String extractEmailIfValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            // Jeton expire, signature invalide ou format incorrect : on ne
            // journalise qu'au niveau debug, car ce cas est courant et normal.
            log.debug("Jeton JWT rejete : {}", e.getMessage());
            return null;
        }
    }
}
