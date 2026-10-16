package com.promptly.backend.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.promptly.backend.ai.OllamaClient.ChatMessage;
import com.promptly.backend.ai.dto.ChatMessageRequest;
import com.promptly.backend.ai.dto.ChatRequest;
import com.promptly.backend.ai.dto.ChatResponse;
import com.promptly.backend.common.exception.BusinessRuleException;

/**
 * Chatbot general Promptly via Ollama ({@code /api/chat}).
 */
@Service
public class AiChatService {

    private static final String SYSTEM_PROMPT = """
            Tu es l'Assistant IA de Promptly, une plateforme d'apprentissage de la programmation
            (Java, Spring Boot, Angular, TypeScript, algorithmique).

            Promptly propose : cours et chapitres (PDF, resumes, flashcards), quiz QCM,
            forum d'entraide, reclamations etudiantes, et cet assistant.

            Regles :
            - Reponds en francais, clair et pedagogique.
            - Aide sur le code, les concepts, les erreurs, et l'usage de Promptly.
            - Si tu n'es pas sur, dis-le et propose une piste.
            - Ne invente pas de notes, comptes ou donnees personnelles de l'utilisateur.
            - Reste concis sauf si l'utilisateur demande du detail.
            """;

    private final OllamaClient ollamaClient;

    public AiChatService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    public ChatResponse chat(ChatRequest request) {
        List<ChatMessageRequest> incoming = request.messages();
        if (incoming == null || incoming.isEmpty()) {
            throw new BusinessRuleException("Envoyez au moins un message");
        }

        ChatMessageRequest last = incoming.get(incoming.size() - 1);
        if (!"user".equalsIgnoreCase(last.role())) {
            throw new BusinessRuleException("Le dernier message doit venir de l'utilisateur");
        }

        List<ChatMessage> history = new ArrayList<>();
        history.add(new ChatMessage("system", SYSTEM_PROMPT));

        int start = Math.max(0, incoming.size() - 20);
        for (int i = start; i < incoming.size(); i++) {
            ChatMessageRequest msg = incoming.get(i);
            String role = msg.role().trim().toLowerCase();
            String content = msg.content() == null ? "" : msg.content().trim();
            if (content.isBlank()) {
                continue;
            }
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            history.add(new ChatMessage(role, content));
        }

        if (history.size() < 2) {
            throw new BusinessRuleException("Message utilisateur vide");
        }

        String reply = ollamaClient.chat(history, 0.5, 1200);
        return new ChatResponse(reply, ollamaClient.modelName());
    }
}
