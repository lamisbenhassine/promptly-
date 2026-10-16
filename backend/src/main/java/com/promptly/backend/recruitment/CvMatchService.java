package com.promptly.backend.recruitment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Matching CV / offre via Google Gemini (API gratuite + cle).
 * Cle : https://aistudio.google.com/apikey
 * Fallback : detection lexicale des competences dans le texte du CV.
 */
@Service
public class CvMatchService {

    private static final Logger log = LoggerFactory.getLogger(CvMatchService.class);
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String apiKey;
    private final String model;

    public CvMatchService(
            ObjectMapper objectMapper,
            @Value("${promptly.cv-match.enabled:true}") boolean enabled,
            @Value("${promptly.cv-match.api-key:}") String apiKey,
            @Value("${promptly.cv-match.gemini-model:gemini-2.0-flash}") String model,
            @Value("${promptly.cv-match.gemini-url:https://generativelanguage.googleapis.com}") String baseUrl) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gemini-2.0-flash" : model.trim();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(45));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.replaceAll("/$", ""))
                .requestFactory(factory)
                .build();

        if (!enabled) {
            log.info("Matching CV desactive");
        } else if (this.apiKey.isBlank()) {
            log.warn("promptly.cv-match.api-key vide — matching lexical uniquement "
                    + "(cle gratuite : https://aistudio.google.com/apikey)");
        } else {
            log.info("Matching CV Gemini active (modele {})", this.model);
        }
    }

    public CvMatchResult match(String offerSkills, String offerExperience, String cvText) {
        List<String> required = parseSkills(offerSkills);
        String cv = cvText == null ? "" : cvText;
        String cvLower = cv.toLowerCase(Locale.ROOT);

        if (!required.isEmpty() && enabled && !apiKey.isBlank() && !cv.isBlank()) {
            CvMatchResult gemini = callGemini(required, offerExperience, cv);
            if (gemini != null) {
                return gemini;
            }
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : required) {
            if (containsSkill(cvLower, skill)) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }

        boolean experienceOk = matchExperience(offerExperience, cvLower);
        double skillScore = required.isEmpty()
                ? 100.0
                : (matched.size() * 100.0) / required.size();
        double experienceBoost = offerExperience == null || offerExperience.isBlank()
                ? 0
                : (experienceOk ? 10.0 : -5.0);
        double total = Math.max(0, Math.min(100, skillScore + experienceBoost));

        return new CvMatchResult(
                Math.round(total * 10.0) / 10.0,
                matched,
                missing,
                experienceOk,
                "lexical");
    }

    private CvMatchResult callGemini(List<String> requiredSkills, String experience, String cvText) {
        try {
            String clipped = cvText.length() > 8000 ? cvText.substring(0, 8000) : cvText;
            String skillsList = String.join(", ", requiredSkills);
            String exp = experience == null || experience.isBlank() ? "(non precisee)" : experience.trim();

            String prompt = """
                    Tu es un assistant de recrutement. Compare le CV avec les exigences de l'offre.
                    Competences requises : %s
                    Experience requise : %s

                    Texte du CV :
                    ---
                    %s
                    ---

                    Reponds UNIQUEMENT avec un JSON valide (sans markdown) de la forme :
                    {"matchedSkills":["..."],"missingSkills":["..."],"experienceMatched":true,"scorePercent":75}
                    - matchedSkills : competences clairement presentes dans le CV (sous-ensemble des requises)
                    - missingSkills : competences absentes
                    - experienceMatched : true si l'experience du CV couvre l'exigence
                    - scorePercent : nombre 0 a 100
                    """.formatted(skillsList, exp, clipped);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contents", List.of(
                    Map.of("parts", List.of(Map.of("text", prompt)))
            ));
            body.put("generationConfig", Map.of(
                    "temperature", 0.1,
                    "maxOutputTokens", 512
            ));

            JsonNode root = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null) {
                return null;
            }
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!textNode.isTextual()) {
                log.warn("Gemini matching : reponse sans texte");
                return null;
            }

            String raw = textNode.asText();
            Matcher matcher = JSON_BLOCK.matcher(raw);
            if (!matcher.find()) {
                log.warn("Gemini matching : JSON introuvable dans la reponse");
                return null;
            }

            JsonNode parsed = objectMapper.readTree(matcher.group());
            List<String> matched = readStringList(parsed.path("matchedSkills"));
            List<String> missing = readStringList(parsed.path("missingSkills"));
            // Normaliser par rapport a la liste requise
            List<String> requiredLower = requiredSkills.stream()
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList();
            matched = matched.stream()
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .filter(s -> requiredLower.stream().anyMatch(r -> r.equalsIgnoreCase(s)
                            || r.contains(s.toLowerCase(Locale.ROOT))
                            || s.toLowerCase(Locale.ROOT).contains(r)))
                    .distinct()
                    .toList();
            if (missing.isEmpty() && !requiredSkills.isEmpty()) {
                List<String> finalMatched = matched;
                missing = requiredSkills.stream()
                        .filter(req -> finalMatched.stream().noneMatch(m ->
                                m.equalsIgnoreCase(req)
                                        || m.toLowerCase(Locale.ROOT).contains(req.toLowerCase(Locale.ROOT))
                                        || req.toLowerCase(Locale.ROOT).contains(m.toLowerCase(Locale.ROOT))))
                        .toList();
            }

            boolean experienceOk = parsed.path("experienceMatched").asBoolean(
                    matchExperience(experience, clipped.toLowerCase(Locale.ROOT)));
            double score = parsed.path("scorePercent").asDouble(-1);
            if (score < 0) {
                score = requiredSkills.isEmpty()
                        ? 100.0
                        : (matched.size() * 100.0) / requiredSkills.size();
            }
            score = Math.max(0, Math.min(100, score));

            return new CvMatchResult(
                    Math.round(score * 10.0) / 10.0,
                    matched,
                    missing,
                    experienceOk,
                    "gemini");
        } catch (RestClientException e) {
            log.warn("Gemini matching injoignable : {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Gemini matching reponse invalide : {}", e.getMessage());
            return null;
        }
    }

    private static List<String> readStringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText().trim());
                }
            }
        }
        return values;
    }

    static List<String> parseSkills(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Pattern.compile("[,;\\n|]+")
                .splitAsStream(raw)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.replaceAll("\\s+", " "))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static boolean containsSkill(String cvLower, String skill) {
        String needle = skill.toLowerCase(Locale.ROOT).trim();
        if (needle.length() < 2) {
            return false;
        }
        String escaped = Pattern.quote(needle);
        return Pattern.compile("(?i)(?<!\\p{L})" + escaped + "(?!\\p{L})")
                .matcher(cvLower)
                .find()
                || cvLower.contains(needle);
    }

    static boolean matchExperience(String requiredExperience, String cvLower) {
        if (requiredExperience == null || requiredExperience.isBlank()) {
            return true;
        }
        String req = requiredExperience.toLowerCase(Locale.ROOT);
        var offerYears = Pattern.compile("(\\d+)\\s*(?:ans?|years?|yrs?)")
                .matcher(req);
        Integer needed = null;
        if (offerYears.find()) {
            needed = Integer.parseInt(offerYears.group(1));
        }
        if (needed != null) {
            var cvYears = Pattern.compile("(\\d+)\\s*(?:ans?|years?|yrs?)")
                    .matcher(cvLower);
            int max = 0;
            while (cvYears.find()) {
                max = Math.max(max, Integer.parseInt(cvYears.group(1)));
            }
            if (max >= needed) {
                return true;
            }
        }
        String[] tokens = req.split("[^a-z0-9+#.]+");
        int hits = 0;
        int meaningful = 0;
        for (String token : tokens) {
            if (token.length() < 3) {
                continue;
            }
            meaningful++;
            if (cvLower.contains(token)) {
                hits++;
            }
        }
        return meaningful == 0 || hits >= Math.ceil(meaningful * 0.4);
    }

    static String joinSkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        return String.join(", ", skills);
    }
}
