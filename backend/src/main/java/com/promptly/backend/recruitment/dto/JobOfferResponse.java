package com.promptly.backend.recruitment.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.promptly.backend.recruitment.ContractType;
import com.promptly.backend.recruitment.JobOffer;

public record JobOfferResponse(
        Long id,
        String title,
        String description,
        String skills,
        String experience,
        String location,
        ContractType contractType,
        boolean published,
        LocalDate deadline,
        ApplicantSummary createdBy,
        long applicationCount,
        boolean alreadyApplied,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobOfferResponse from(JobOffer offer, long applicationCount, boolean alreadyApplied) {
        return new JobOfferResponse(
                offer.getId(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getSkills(),
                offer.getExperience(),
                offer.getLocation(),
                offer.getContractType(),
                offer.isPublished(),
                offer.getDeadline(),
                ApplicantSummary.from(offer.getCreatedBy()),
                applicationCount,
                alreadyApplied,
                offer.getCreatedAt(),
                offer.getUpdatedAt());
    }
}
