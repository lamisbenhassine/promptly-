package com.promptly.backend.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

import com.promptly.backend.common.exception.BusinessRuleException;

/**
 * Client HTTP vers Ollama ({@code /api/generate} et {@code /api/chat}).
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
        return generate(prompt, 0.3, 800, false);
    }

    public String generate(String prompt, double temperature, int numPredict) {
        return generate(prompt, temperature, numPredict, false);
    }

    /**
     * @param jsonMode si vrai, demande a Ollama une reponse JSON forcee ({@code format: json}).
     */
    public String generate(String prompt, double temperature, int numPredict, boolean jsonMode) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("options", Map.of(
                    "temperature", temperature,
                    "num_predict", numPredict
            ));
            if (jsonMode) {
                body.put("format", "json");
            }

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
            throw unreachable();
        }
    }

    /**
     * Chat multi-tours via {@code /api/chat} (roles system / user / assistant).
     */
    public String chat(List<ChatMessage> messages, double temperature, int numPredict) {
        if (messages == null || messages.isEmpty()) {
            throw new BusinessRuleException("Aucun message a envoyer a Ollama");
        }
        try {
            List<Map<String, String>> payloadMessages = new ArrayList<>();
            for (ChatMessage message : messages) {
                payloadMessages.add(Map.of(
                        "role", message.role(),
                        "content", message.content()
                ));
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", payloadMessages);
            body.put("stream", false);
            body.put("options", Map.of(
                    "temperature", temperature,
                    "num_predict", numPredict
            ));

            OllamaChatResponse response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response == null || response.message() == null
                    || response.message().content() == null
                    || response.message().content().isBlank()) {
                throw new BusinessRuleException("Ollama a renvoye une reponse vide");
            }
            return response.message().content().trim();
        } catch (RestClientException e) {
            log.warn("Echec chat Ollama ({}) : {}", model, e.getMessage());
            throw unreachable();
        }
    }

    public String modelName() {
        return model;
    }

    private BusinessRuleException unreachable() {
        return new BusinessRuleException(
                "Ollama est injoignable. Verifiez que le service tourne "
                        + "(ollama serve) avec le modele " + model + ".");
    }

    public record ChatMessage(String role, String content) {
    }

    /** Sous-ensemble de la reponse Ollama /api/generate. */
    public record OllamaGenerateResponse(String response) {
    }

    /** Sous-ensemble de la reponse Ollama /api/chat. */
    public record OllamaChatResponse(OllamaChatMessage message) {
    }

    public record OllamaChatMessage(String role, String content) {
    }
}
