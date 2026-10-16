package com.promptly.backend.recruitment;

import java.util.List;

/** Resultat du matching CV vs competences / experience de l'offre. */
public record CvMatchResult(
        double scorePercent,
        List<String> matchedSkills,
        List<String> missingSkills,
        boolean experienceMatched,
        String provider
) {
}
