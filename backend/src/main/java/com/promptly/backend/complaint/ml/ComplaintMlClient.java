package com.promptly.backend.complaint.ml;

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

import com.promptly.backend.complaint.ComplaintCategory;
import com.promptly.backend.complaint.ComplaintPriority;
import com.promptly.backend.complaint.ComplaintUrgency;

/**
 * Client vers le micro-service Python de classement des reclamations
 * (type, urgence, priorite uniquement).
 */
@Service
public class ComplaintMlClient {

    private static final Logger log = LoggerFactory.getLogger(ComplaintMlClient.class);

    private static final Pattern[] URGENT_PATTERNS = {
            Pattern.compile("\\burgent\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\basap\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bcritique\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bbloquant\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdeadline\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bafter\\s+\\d+\\s*min", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdans\\s+\\d+\\s*min", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d+\\s*min(?:ute)?s?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bget\\s+zero\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bexamen\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bexam\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("can'?t\\s+open", Pattern.CASE_INSENSITIVE),
            Pattern.compile("cannot\\s+open", Pattern.CASE_INSENSITIVE),
            Pattern.compile("impossible\\s+d[' ]ouvrir", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ne\\s+(marche|fonctionne)\\s+plus", Pattern.CASE_INSENSITIVE),
            Pattern.compile("fix\\s+this\\s+(pls|please)", Pattern.CASE_INSENSITIVE)
    };

    private static final Pattern[] CALM_PATTERNS = {
            Pattern.compile("sans\\s+urgence", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bno\\s+rush\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("not\\s+urgent", Pattern.CASE_INSENSITIVE),
            Pattern.compile("when\\s+you\\s+(can|have\\s+time)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("quand\\s+vous\\s+pourrez", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsuggestion\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("petite\\s+remarque", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pour\\s+information", Pattern.CASE_INSENSITIVE),
            Pattern.compile("low\\s+priority", Pattern.CASE_INSENSITIVE)
    };

    private final RestClient restClient;
    private final boolean enabled;

    public ComplaintMlClient(
            @Value("${promptly.complaints-ml.base-url:http://localhost:8091}") String baseUrl,
            @Value("${promptly.complaints-ml.enabled:true}") boolean enabled) {
        this.enabled = enabled;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/$", ""))
                .requestFactory(factory)
                .build();
    }

    public ComplaintMlPrediction classify(String subject, String description) {
        if (enabled) {
            try {
                ClassifyResponse response = restClient.post()
                        .uri("/classify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "subject", subject == null ? "" : subject,
                                "description", description == null ? "" : description))
                        .retrieve()
                        .body(ClassifyResponse.class);
                if (response != null && response.type() != null) {
                    return new ComplaintMlPrediction(
                            parseCategory(response.type()),
                            parseUrgency(response.urgency()),
                            parsePriority(response.priority()),
                            response.confidence());
                }
            } catch (RestClientException e) {
                log.warn("Service ML reclamations injoignable, heuristique locale : {}", e.getMessage());
            }
        }
        return heuristic(subject, description);
    }

    ComplaintMlPrediction heuristic(String subject, String description) {
        String text = ((subject == null ? "" : subject) + "\n" + (description == null ? "" : description))
                .toLowerCase(Locale.ROOT);

        ComplaintCategory category = detectCategory(text);
        boolean calm = matchesAny(text, CALM_PATTERNS);
        boolean urgentSignal = matchesAny(text, URGENT_PATTERNS);

        ComplaintUrgency urgency;
        ComplaintPriority priority;
        if (calm && !urgentSignal) {
            urgency = ComplaintUrgency.NON_URGENT;
            priority = ComplaintPriority.BASSE;
        } else if (urgentSignal) {
            urgency = ComplaintUrgency.URGENT;
            boolean high = containsAny(text, "deadline", "examen", "exam", "get zero", "critique", "bloquant")
                    || text.matches("(?s).*\\d+\\s*min.*");
            priority = high ? ComplaintPriority.HAUTE : ComplaintPriority.MOYENNE;
        } else {
            urgency = ComplaintUrgency.NON_URGENT;
            priority = ComplaintPriority.MOYENNE;
        }

        return new ComplaintMlPrediction(category, urgency, priority, 0.64);
    }

    private static ComplaintCategory detectCategory(String text) {
        if (containsAny(text, "bug", "erreur", "error", "crash", "quiz", "pdf", "javascript", "api",
                "can't open", "cannot open", "doesn't work", "ne marche", "ne fonctionne",
                "impossible d'ouvrir", "impossible d ouvrir")) {
            return ComplaintCategory.TECHNIQUE;
        }
        if (containsAny(text, "hors ligne", "offline", "indisponible", "unavailable", "down", "timeout",
                "ne repond", "ne répond")) {
            return ComplaintCategory.DISPONIBILITE;
        }
        if (containsAny(text, "compte", "account", "password", "mot de passe", "login", "connexion",
                "profil", "session")) {
            return ComplaintCategory.COMPTE;
        }
        if (containsAny(text, "pedagog", "explication", "confus", "clarif", "example", "exemple",
                "flashcard", "notion", "contenu", "difficult")) {
            return ComplaintCategory.PEDAGOGIQUE;
        }
        return ComplaintCategory.AUTRE;
    }

    private static boolean matchesAny(String text, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static ComplaintCategory parseCategory(String raw) {
        try {
            return ComplaintCategory.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return ComplaintCategory.AUTRE;
        }
    }

    private static ComplaintUrgency parseUrgency(String raw) {
        try {
            return ComplaintUrgency.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return ComplaintUrgency.NON_URGENT;
        }
    }

    private static ComplaintPriority parsePriority(String raw) {
        try {
            return ComplaintPriority.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return ComplaintPriority.MOYENNE;
        }
    }

    public record ClassifyResponse(
            String type,
            String urgency,
            String priority,
            double confidence
    ) {
    }
}
