package com.promptly.backend.recruitment;

import java.time.Instant;

import com.promptly.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "job_applications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_job_application_offer_applicant",
                columnNames = {"offer_id", "applicant_id"}
        )
)
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private JobOffer offer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    /** Chemin relatif du CV PDF (uploads/...). */
    @Column(nullable = false, length = 400)
    private String cvPath;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.ENVOYEE;

    /** Score de matching CV / offre (0..100). */
    private Double matchScore;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String matchedSkills;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    private Boolean experienceMatched;

    @Column(length = 40)
    private String matchProvider;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected JobApplication() {
    }

    public JobApplication(JobOffer offer, User applicant, String cvPath, String message) {
        this.offer = offer;
        this.applicant = applicant;
        this.cvPath = cvPath;
        this.message = message;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public JobOffer getOffer() {
        return offer;
    }

    public User getApplicant() {
        return applicant;
    }

    public String getCvPath() {
        return cvPath;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(String matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public String getMissingSkills() {
        return missingSkills;
    }

    public void setMissingSkills(String missingSkills) {
        this.missingSkills = missingSkills;
    }

    public Boolean getExperienceMatched() {
        return experienceMatched;
    }

    public void setExperienceMatched(Boolean experienceMatched) {
        this.experienceMatched = experienceMatched;
    }

    public String getMatchProvider() {
        return matchProvider;
    }

    public void setMatchProvider(String matchProvider) {
        this.matchProvider = matchProvider;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
