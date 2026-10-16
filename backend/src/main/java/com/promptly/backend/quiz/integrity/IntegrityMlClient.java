package com.promptly.backend.quiz.integrity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client vers le micro-service Python d'integrite (guess vs know).
 * Si le service est down, un heuristique local est utilise.
 */
@Service
public class IntegrityMlClient {

    private static final Logger log = LoggerFactory.getLogger(IntegrityMlClient.class);

    private final RestClient restClient;
    private final boolean enabled;

    public IntegrityMlClient(
            @Value("${promptly.integrity.base-url:http://localhost:8090}") String baseUrl,
            @Value("${promptly.integrity.enabled:true}") boolean enabled) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/$", ""))
                .requestFactory(factory)
                .build();
    }

    public List<IntegrityPrediction> predict(List<IntegrityFeature> features) {
        if (features == null || features.isEmpty()) {
            return List.of();
        }
        List<IntegrityPrediction> predictions = null;
        if (enabled) {
            try {
                Map<String, Object> body = Map.of("items", features);
                PredictResponse response = restClient.post()
                        .uri("/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(PredictResponse.class);
                if (response != null && response.predictions() != null && !response.predictions().isEmpty()) {
                    predictions = response.predictions();
                }
            } catch (RestClientException e) {
                log.warn("Service ML integrite injoignable, heuristique locale : {}", e.getMessage());
            }
        }
        if (predictions == null) {
            predictions = heuristic(features);
        }
        return enforceWrongAnswerIsGuess(features, predictions);
    }

    /**
     * Regle metier : une mauvaise reponse ne peut jamais etre labellisee KNOW.
     */
    private List<IntegrityPrediction> enforceWrongAnswerIsGuess(
            List<IntegrityFeature> features,
            List<IntegrityPrediction> predictions) {
        Map<Long, Boolean> correctByQuestion = new HashMap<>();
        for (IntegrityFeature feature : features) {
            correctByQuestion.put(feature.questionId(), feature.isCorrect());
        }
        List<IntegrityPrediction> out = new ArrayList<>(predictions.size());
        for (IntegrityPrediction prediction : predictions) {
            boolean correct = correctByQuestion.getOrDefault(prediction.questionId(), false);
            if (!correct && "KNOW".equalsIgnoreCase(prediction.label())) {
                out.add(new IntegrityPrediction(
                        prediction.questionId(),
                        "GUESS",
                        Math.max(prediction.confidence(), 0.75)));
            } else {
                out.add(prediction);
            }
        }
        return out;
    }

    /** Fallback simple sans Python (rapide + changements = GUESS). */
    private List<IntegrityPrediction> heuristic(List<IntegrityFeature> features) {
        List<IntegrityPrediction> out = new ArrayList<>();
        for (IntegrityFeature f : features) {
            // Mauvaise reponse => toujours GUESS (ne "connait" pas la reponse).
            boolean guess = !f.isCorrect()
                    || f.timeSpentMs() < 4000
                    || (f.selectionChanges() >= 3 && f.timeSpentMs() < 12000)
                    || (f.timeSpentMs() < f.avgTimeMs() * 0.35 && f.avgTimeMs() > 0);
            out.add(new IntegrityPrediction(
                    f.questionId(),
                    guess ? "GUESS" : "KNOW",
                    guess ? 0.72 : 0.68));
        }
        return out;
    }

    public record PredictResponse(List<IntegrityPrediction> predictions) {
    }
}
