package com.promptly.backend.ai;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.promptly.backend.common.exception.BusinessRuleException;

/**
 * Client HTTP vers Ollama ({@code /api/generate}).
 * Ollama doit tourner localement (ex. {@code http://localhost:11434}).
 */
@Service
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestClient restClient;
    private final String model;

    public OllamaClient(
            @Value("${promptly.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${promptly.ollama.model:llama3.2:3b}") String model,
            @Value("${promptly.ollama.timeout-seconds:180}") int timeoutSeconds) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(30, timeoutSeconds)));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/$", ""))
                .requestFactory(factory)
                .build();
        this.model = model;
    }

    public String generate(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "prompt", prompt,
                    "stream", false,
                    "options", Map.of(
                            "temperature", 0.3,
                            "num_predict", 800
                    )
            );

            OllamaGenerateResponse response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OllamaGenerateResponse.class);

            if (response == null || response.response() == null || response.response().isBlank()) {
                throw new BusinessRuleException("Ollama a renvoye une reponse vide");
            }
            return response.response().trim();
        } catch (RestClientException e) {
            log.warn("Echec appel Ollama ({}) : {}", model, e.getMessage());
            throw new BusinessRuleException(
                    "Ollama est injoignable. Verifiez que le service tourne "
                            + "(ollama serve) avec le modele " + model + ".");
        }
    }

    public String modelName() {
        return model;
    }

    /** Sous-ensemble de la reponse Ollama /api/generate. */
    public record OllamaGenerateResponse(String response) {
    }
}
