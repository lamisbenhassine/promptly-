package com.promptly.backend.complaint;

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

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String subject;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ComplaintCategory category = ComplaintCategory.AUTRE;

    /** Urgence predite par le ML (visible admin seulement). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ComplaintUrgency mlUrgency;

    /** Priorite predite par le ML (visible admin seulement). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ComplaintPriority mlPriority;

    /** Confiance moyenne du classifieur (0..1). */
    private Double mlConfidence;

    /** Emotion predite (style d'ecriture) — visible admin seulement. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ComplaintEmotion mlEmotion;

    private Double mlEmotionConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComplaintStatus status = ComplaintStatus.OUVERTE;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String adminResponse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_id")
    private User handledBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant resolvedAt;

    protected Complaint() {
    }

    public Complaint(String subject, String description, ComplaintCategory category, User author) {
        this.subject = subject;
        this.description = description;
        this.category = category;
        this.author = author;
    }

    public void applyMlPrediction(ComplaintCategory category, ComplaintUrgency urgency,
                                  ComplaintPriority priority, Double confidence,
                                  ComplaintEmotion emotion, Double emotionConfidence) {
        this.category = category;
        this.mlUrgency = urgency;
        this.mlPriority = priority;
        this.mlConfidence = confidence;
        this.mlEmotion = emotion;
        this.mlEmotionConfidence = emotionConfidence;
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public ComplaintUrgency getMlUrgency() {
        return mlUrgency;
    }

    public ComplaintPriority getMlPriority() {
        return mlPriority;
    }

    public Double getMlConfidence() {
        return mlConfidence;
    }

    public ComplaintEmotion getMlEmotion() {
        return mlEmotion;
    }

    public Double getMlEmotionConfidence() {
        return mlEmotionConfidence;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public User getAuthor() {
        return author;
    }

    public String getAdminResponse() {
        return adminResponse;
    }

    public void setAdminResponse(String adminResponse) {
        this.adminResponse = adminResponse;
    }

    public User getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(User handledBy) {
        this.handledBy = handledBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
