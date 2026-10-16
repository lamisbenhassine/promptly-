package com.promptly.backend.complaint.emotion;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.promptly.backend.complaint.ComplaintEmotion;

/**
 * Detection d'emotion a partir du texte de la reclamation.
 * API Hugging Face + lexique local (injures, frustration, etc.).
 */
@Service
public class ComplaintEmotionService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintEmotionService.class);

    private static final String DEFAULT_MODEL =
            "j-hartmann/emotion-english-distilroberta-base";

    /** Signaux forts de colere / frustration (FR + EN, fautes courantes). */
    private static final Pattern ANGRY = Pattern.compile(
            "\\b(fuck(?:ing|er)?|f\\*+ck|shit|damn|dam\\b|crap|stupid|idiot|"
                    + "hate|angry|furious|pissed|ridiculous|bullshit|"
                    + "merde|putain|bordel|enerve|énervé|furieux|colere|colère|"
                    + "scandaleux|inadmissible|nul|wtf|omg)\\b"
                    + "|!{2,}",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FEARFUL = Pattern.compile(
            "\\b(afraid|scared|worried|anxious|panic|deadline|inquiet|inquiète|"
                    + "peur|angoiss\\w*|get\\s+zero|aurai\\s+z[eé]ro)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SAD = Pattern.compile(
            "\\b(sad|disappointed|triste|d[eé][cç]u|d[eé]sesp[eé]r\\w*|unfair)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HAPPY = Pattern.compile(
            "\\b(thanks|thank\\s+you|happy|great|awesome|merci|content|ravi|"
                    + "super|parfait|excellent|love)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SURPRISED = Pattern.compile(
            "\\b(surprised|unexpected|surpris|étonn\\w*|etonn\\w*|inattendu)\\b",
            Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;
    private final boolean enabled;
    private final String token;
    private final String model;

    public ComplaintEmotionService(
            @Value("${promptly.emotion.enabled:true}") boolean enabled,
            @Value("${promptly.emotion.hf-token:}") String token,
            @Value("${promptly.emotion.hf-model:" + DEFAULT_MODEL + "}") String model) {
        this.enabled = enabled;
        this.token = token == null ? "" : token.trim();
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = RestClient.builder()
                .baseUrl("https://api-inference.huggingface.co")
                .requestFactory(factory)
                .build();

        if (this.token.isBlank()) {
            log.warn("promptly.emotion.hf-token vide — emotion via lexique local uniquement");
        } else {
            log.info("Emotion Hugging Face activee (modele {})", this.model);
        }
    }

    public ComplaintEmotionResult detect(String subject, String description) {
        String text = ((subject == null ? "" : subject) + "\n"
                + (description == null ? "" : description)).trim();
        if (text.isBlank()) {
            return new ComplaintEmotionResult(ComplaintEmotion.NEUTRAL, 0.5);
        }

        // Lexique d'abord pour les signaux tres clairs (injures, etc.).
        ComplaintEmotionResult lexical = lexicon(text);
        if (lexical.emotion() != ComplaintEmotion.NEUTRAL && lexical.confidence() >= 0.8) {
            return lexical;
        }

        if (enabled && !token.isBlank()) {
            try {
                ComplaintEmotionResult api = callHuggingFace(text);
                if (api != null) {
                    return api;
                }
            } catch (RestClientException e) {
                log.warn("API emotion Hugging Face injoignable, lexique local : {}", e.getMessage());
            } catch (RuntimeException e) {
                log.warn("Reponse emotion Hugging Face invalide, lexique local : {}", e.getMessage());
            }
        }

        return lexical;
    }

    private ComplaintEmotionResult callHuggingFace(String text) {
        String clipped = text.length() > 1500 ? text.substring(0, 1500) : text;

        RestClient.RequestBodySpec request = restClient.post()
                .uri("/models/{model}", model)
                .contentType(MediaType.APPLICATION_JSON);
        if (!token.isBlank()) {
            request = request.header("Authorization", "Bearer " + token);
        }

        JsonNode root = request
                .body(Map.of("inputs", clipped))
                .retrieve()
                .body(JsonNode.class);
        if (root == null) {
            return null;
        }
        if (root.has("error")) {
            log.warn("Hugging Face emotion error: {}", root.get("error").asText());
            return null;
        }

        JsonNode scores = root;
        if (root.isArray() && !root.isEmpty() && root.get(0).isArray()) {
            scores = root.get(0);
        }
        if (!scores.isArray() || scores.isEmpty()) {
            return null;
        }

        String bestLabel = null;
        double bestScore = -1;
        for (JsonNode item : scores) {
            double score = item.path("score").asDouble(-1);
            if (score > bestScore) {
                bestScore = score;
                bestLabel = item.path("label").asText(null);
            }
        }
        if (bestLabel == null) {
            return null;
        }
        return new ComplaintEmotionResult(mapLabel(bestLabel), bestScore);
    }

    private static ComplaintEmotion mapLabel(String raw) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "anger", "disgust" -> ComplaintEmotion.ANGRY;
            case "joy", "happiness", "love" -> ComplaintEmotion.HAPPY;
            case "sadness" -> ComplaintEmotion.SAD;
            case "fear" -> ComplaintEmotion.FEARFUL;
            case "surprise" -> ComplaintEmotion.SURPRISED;
            default -> ComplaintEmotion.NEUTRAL;
        };
    }

    ComplaintEmotionResult lexicon(String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        if (ANGRY.matcher(lower).find()) {
            return new ComplaintEmotionResult(ComplaintEmotion.ANGRY, 0.9);
        }
        if (FEARFUL.matcher(lower).find()) {
            return new ComplaintEmotionResult(ComplaintEmotion.FEARFUL, 0.8);
        }
        if (SAD.matcher(lower).find()) {
            return new ComplaintEmotionResult(ComplaintEmotion.SAD, 0.78);
        }
        if (HAPPY.matcher(lower).find()) {
            return new ComplaintEmotionResult(ComplaintEmotion.HAPPY, 0.82);
        }
        if (SURPRISED.matcher(lower).find()) {
            return new ComplaintEmotionResult(ComplaintEmotion.SURPRISED, 0.75);
        }
        return new ComplaintEmotionResult(ComplaintEmotion.NEUTRAL, 0.55);
    }
}
