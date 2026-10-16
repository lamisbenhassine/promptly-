package com.promptly.backend.recruitment.dto;

import java.time.Instant;

import com.promptly.backend.recruitment.ApplicationStatus;
import com.promptly.backend.recruitment.JobApplication;

public record JobApplicationResponse(
        Long id,
        Long offerId,
        String offerTitle,
        ApplicantSummary applicant,
        String cvUrl,
        String message,
        ApplicationStatus status,
        Double matchScore,
        String matchedSkills,
        String missingSkills,
        Boolean experienceMatched,
        String matchProvider,
        Instant createdAt
) {
    public static JobApplicationResponse from(JobApplication application) {
        String path = application.getCvPath();
        return new JobApplicationResponse(
                application.getId(),
                application.getOffer().getId(),
                application.getOffer().getTitle(),
                ApplicantSummary.from(application.getApplicant()),
                path == null || path.isBlank() ? null : "/api/files/" + path,
                application.getMessage(),
                application.getStatus(),
                application.getMatchScore(),
                application.getMatchedSkills(),
                application.getMissingSkills(),
                application.getExperienceMatched(),
                application.getMatchProvider(),
                application.getCreatedAt());
    }
}
