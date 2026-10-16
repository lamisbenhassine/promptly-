package com.promptly.backend.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.promptly.backend.common.exception.BusinessRuleException;

/**
 * Verifie un jeton d'identite Google (GIS) aupres des cles publiques Google.
 *
 * Le navigateur envoie le {@code credential} ; le serveur le valide avec le
 * Client ID configure. On ne fait jamais confiance au profil sans cette etape.
 */
@Service
public class GoogleTokenService {

    private final String clientId;
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenService(@Value("${promptly.google.client-id:}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        if (this.clientId.isEmpty()) {
            this.verifier = null;
        } else {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(this.clientId))
                    .build();
        }
    }

    public boolean isConfigured() {
        return verifier != null;
    }

    public GoogleProfile verify(String idToken) {
        if (verifier == null) {
            throw new BusinessRuleException(
                    "Connexion Google non configuree cote serveur "
                            + "(PROMPTLY_GOOGLE_CLIENT_ID manquant).");
        }

        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new BusinessRuleException("Jeton Google invalide ou expire");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            String email = payload.getEmail();
            if (email == null || email.isBlank()) {
                throw new BusinessRuleException("Le compte Google ne fournit pas d'adresse electronique");
            }
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BusinessRuleException("L'adresse Google n'est pas verifiee");
            }

            String givenName = (String) payload.get("given_name");
            String familyName = (String) payload.get("family_name");
            String name = (String) payload.get("name");

            return new GoogleProfile(
                    email.trim().toLowerCase(),
                    resolveFirstName(givenName, name, email),
                    resolveLastName(familyName, name));
        } catch (GeneralSecurityException | IOException e) {
            throw new BusinessRuleException("Impossible de verifier le jeton Google");
        }
    }

    private static String resolveFirstName(String givenName, String fullName, String email) {
        if (givenName != null && !givenName.isBlank()) {
            return truncate(givenName.trim(), 50);
        }
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            return truncate(parts[0], 50);
        }
        String local = email.split("@")[0];
        return truncate(local.isBlank() ? "Utilisateur" : local, 50);
    }

    private static String resolveLastName(String familyName, String fullName) {
        if (familyName != null && !familyName.isBlank()) {
            return truncate(familyName.trim(), 50);
        }
        if (fullName != null && !fullName.isBlank()) {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (parts.length > 1) {
                return truncate(parts[1], 50);
            }
        }
        return "Google";
    }

    private static String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Profil extrait d'un jeton Google valide. */
    public record GoogleProfile(String email, String firstName, String lastName) {
    }
}
