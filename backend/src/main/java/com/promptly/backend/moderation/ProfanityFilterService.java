package com.promptly.backend.moderation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Remplace les insultes par *** dans le forum (sujets + commentaires).
 * API Sightengine + lexique local FR/EN en secours.
 */
@Service
public class ProfanityFilterService {

    private static final Logger log = LoggerFactory.getLogger(ProfanityFilterService.class);
    private static final String MASK = "***";

    private static final Pattern LOCAL_BAD = Pattern.compile(
            "\\b(fuck(?:ing|er)?|f\\*+ck|shit|bitch|asshole|bastard|dick|cunt|slut|"
                    + "damn|crap|bullshit|wtf|"
                    + "putain|merde|connard|connasse|salope|encul\\w*|fdp|"
                    + "bordel|nique|niquer)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final RestClient restClient;
    private final boolean enabled;
    private final String apiUser;
    private final String apiSecret;

    public ProfanityFilterService(
            @Value("${promptly.profanity.enabled:true}") boolean enabled,
            @Value("${promptly.profanity.api-user:}") String apiUser,
            @Value("${promptly.profanity.api-secret:}") String apiSecret,
            @Value("${promptly.profanity.base-url:https://api.sightengine.com}") String baseUrl) {
        this.enabled = enabled;
        this.apiUser = apiUser == null ? "" : apiUser.trim();
        this.apiSecret = apiSecret == null ? "" : apiSecret.trim();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/$", ""))
                .requestFactory(factory)
                .build();

        if (this.apiUser.isBlank() || this.apiSecret.isBlank()) {
            log.warn("Sightengine api-user/api-secret vides — censure lexique local uniquement");
        } else {
            log.info("Censure profanite Sightengine active (remplacement ***)");
        }
    }

    /** Remplace les gros mots par *** ; laisse le texte intact si rien a censurer. */
    public String censor(String text) {
        if (!enabled || text == null || text.isBlank()) {
            return text;
        }

        String censored = text;
        if (!apiUser.isBlank() && !apiSecret.isBlank()) {
            try {
                String fromApi = censorWithSightengine(text);
                if (fromApi != null) {
                    censored = fromApi;
                }
            } catch (RestClientException e) {
                log.warn("API Sightengine injoignable, lexique local : {}", e.getMessage());
            } catch (RuntimeException e) {
                log.warn("Reponse Sightengine invalide, lexique local : {}", e.getMessage());
            }
        }

        return censorLocal(censored);
    }

    private String censorWithSightengine(String text) {
        String clipped = text.length() > 1500 ? text.substring(0, 1500) : text;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("text", clipped);
        form.add("lang", "en,fr");
        form.add("mode", "rules");
        form.add("api_user", apiUser);
        form.add("api_secret", apiSecret);

        JsonNode root = restClient.post()
                .uri("/1.0/text/check.json")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (root == null || !"success".equalsIgnoreCase(root.path("status").asText(""))) {
            return null;
        }

        JsonNode matches = root.path("profanity").path("matches");
        if (!matches.isArray() || matches.isEmpty()) {
            return text;
        }

        // Remplacer par index (du plus a droite pour ne pas decaler les positions).
        List<int[]> spans = new ArrayList<>();
        for (JsonNode match : matches) {
            int start = match.path("start").asInt(-1);
            int end = match.path("end").asInt(-1);
            if (start >= 0 && end > start && end <= text.length()) {
                spans.add(new int[] { start, end });
            } else {
                String word = match.path("match").asText("");
                if (!word.isBlank()) {
                    int idx = text.toLowerCase(Locale.ROOT).indexOf(word.toLowerCase(Locale.ROOT));
                    if (idx >= 0) {
                        spans.add(new int[] { idx, idx + word.length() });
                    }
                }
            }
        }

        if (spans.isEmpty()) {
            return text;
        }

        spans.sort(Comparator.comparingInt((int[] s) -> s[0]).reversed());
        StringBuilder sb = new StringBuilder(text);
        for (int[] span : spans) {
            sb.replace(span[0], span[1], MASK);
        }
        return sb.toString();
    }

    private String censorLocal(String text) {
        Matcher matcher = LOCAL_BAD.matcher(text);
        return matcher.replaceAll(MASK);
    }
}
