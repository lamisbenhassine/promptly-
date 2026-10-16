package com.promptly.backend.moderation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
 * Auto-correction a la publication (posts + commentaires forum)
 * via TextGears (API avec cle gratuite) :
 *   https://textgears.com/signup.php  → obtenir la cle
 *   https://api.textgears.com/grammar
 *
 * Optionnel : Bing Spell Check Azure (provider=bing).
 */
@Service
public class SpellCorrectService {

    private static final Logger log = LoggerFactory.getLogger(SpellCorrectService.class);

    private final RestClient textGearsClient;
    private final RestClient bingClient;
    private final boolean enabled;
    private final String language;
    private final String provider;
    private final String apiKey;

    public SpellCorrectService(
            @Value("${promptly.spellcheck.enabled:true}") boolean enabled,
            @Value("${promptly.spellcheck.language:fr-FR}") String language,
            @Value("${promptly.spellcheck.provider:textgears}") String provider,
            @Value("${promptly.spellcheck.api-key:}") String apiKey,
            @Value("${promptly.spellcheck.textgears-url:https://api.textgears.com}") String textGearsUrl,
            @Value("${promptly.spellcheck.bing-url:https://api.bing.microsoft.com}") String bingUrl) {
        this.enabled = enabled;
        this.language = language == null || language.isBlank() ? "fr-FR" : language.trim();
        this.provider = provider == null ? "textgears" : provider.trim().toLowerCase();
        this.apiKey = apiKey == null ? "" : apiKey.trim();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(12));

        this.textGearsClient = RestClient.builder()
                .baseUrl(textGearsUrl.replaceAll("/$", ""))
                .requestFactory(factory)
                .build();
        this.bingClient = RestClient.builder()
                .baseUrl(bingUrl.replaceAll("/$", ""))
                .requestFactory(factory)
                .build();

        if (this.apiKey.isBlank()) {
            log.warn("Auto-correct desactive : promptly.spellcheck.api-key manquant "
                    + "(creer une cle gratuite sur https://textgears.com/signup.php)");
        } else if ("bing".equals(this.provider)) {
            log.info("Auto-correct Bing Spell Check active (cle Azure)");
        } else {
            log.info("Auto-correct TextGears active (API avec token) — {}", textGearsUrl);
        }
    }

    public String correct(String text) {
        if (!enabled || text == null || text.isBlank() || apiKey.isBlank()) {
            return text;
        }
        if ("bing".equals(provider)) {
            return correctWithBing(text);
        }
        return correctWithTextGears(text);
    }

    private String correctWithTextGears(String text) {
        try {
            String clipped = text.length() > 10000 ? text.substring(0, 10000) : text;

            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("key", apiKey);
            form.add("text", clipped);
            form.add("language", language);

            JsonNode root = textGearsClient.post()
                    .uri("/grammar")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || !root.path("status").asBoolean(false)) {
                String description = root == null ? "reponse vide" : root.path("description").asText("erreur");
                log.warn("TextGears a refuse la requete : {}", description);
                return text;
            }

            JsonNode errors = root.path("response").path("errors");
            if (!errors.isArray() || errors.isEmpty()) {
                return text;
            }

            List<Replacement> replacements = new ArrayList<>();
            for (JsonNode error : errors) {
                JsonNode better = error.path("better");
                if (!better.isArray() || better.isEmpty()) {
                    continue;
                }
                String value = better.get(0).asText(null);
                if (value == null || value.isBlank()) {
                    continue;
                }
                int offset = error.path("offset").asInt(-1);
                int length = error.path("length").asInt(-1);
                if (offset < 0 || length <= 0 || offset + length > text.length()) {
                    continue;
                }
                if (text.substring(offset, offset + length).contains("*")) {
                    continue;
                }
                replacements.add(new Replacement(offset, length, value));
            }
            return applyReplacements(text, replacements);
        } catch (RestClientException e) {
            log.warn("TextGears injoignable : {}", e.getMessage());
            return text;
        } catch (RuntimeException e) {
            log.warn("TextGears reponse invalide : {}", e.getMessage());
            return text;
        }
    }

    private String correctWithBing(String text) {
        try {
            String clipped = text.length() > 10000 ? text.substring(0, 10000) : text;
            JsonNode root = bingClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v7.0/spellcheck")
                            .queryParam("mode", "spell")
                            .queryParam("mkt", "fr-FR")
                            .build())
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("text=" + java.net.URLEncoder.encode(clipped, java.nio.charset.StandardCharsets.UTF_8))
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return text;
            }
            JsonNode flagged = root.path("flaggedTokens");
            if (!flagged.isArray() || flagged.isEmpty()) {
                return text;
            }

            List<Replacement> replacements = new ArrayList<>();
            for (JsonNode token : flagged) {
                JsonNode suggestions = token.path("suggestions");
                if (!suggestions.isArray() || suggestions.isEmpty()) {
                    continue;
                }
                String value = suggestions.get(0).path("suggestion").asText(null);
                if (value == null || value.isBlank()) {
                    continue;
                }
                int offset = token.path("offset").asInt(-1);
                String tokenText = token.path("token").asText("");
                int length = tokenText.length();
                if (offset < 0 || length <= 0 || offset + length > text.length()) {
                    continue;
                }
                if (text.substring(offset, offset + length).contains("*")) {
                    continue;
                }
                replacements.add(new Replacement(offset, length, value));
            }
            return applyReplacements(text, replacements);
        } catch (RestClientException e) {
            log.warn("Bing Spell Check injoignable : {}", e.getMessage());
            return text;
        } catch (RuntimeException e) {
            log.warn("Bing Spell Check reponse invalide : {}", e.getMessage());
            return text;
        }
    }

    private static String applyReplacements(String text, List<Replacement> replacements) {
        if (replacements.isEmpty()) {
            return text;
        }
        replacements.sort(Comparator.comparingInt(Replacement::offset).reversed());
        StringBuilder sb = new StringBuilder(text);
        for (Replacement replacement : replacements) {
            sb.replace(
                    replacement.offset(),
                    replacement.offset() + replacement.length(),
                    replacement.value());
        }
        return sb.toString();
    }

    private record Replacement(int offset, int length, String value) {
    }
}
