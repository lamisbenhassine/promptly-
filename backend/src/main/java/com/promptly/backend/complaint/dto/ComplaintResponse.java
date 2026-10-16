package com.promptly.backend.complaint.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.promptly.backend.complaint.Complaint;
import com.promptly.backend.complaint.ComplaintCategory;
import com.promptly.backend.complaint.ComplaintEmotion;
import com.promptly.backend.complaint.ComplaintPriority;
import com.promptly.backend.complaint.ComplaintStatus;
import com.promptly.backend.complaint.ComplaintUrgency;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComplaintResponse(
        Long id,
        String subject,
        String description,
        ComplaintCategory category,
        ComplaintStatus status,
        AuthorSummary author,
        String adminResponse,
        AuthorSummary handledBy,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        ComplaintUrgency mlUrgency,
        ComplaintPriority mlPriority,
        Double mlConfidence,
        ComplaintEmotion mlEmotion,
        Double mlEmotionConfidence
) {
    public static ComplaintResponse from(Complaint complaint, boolean revealMl) {
        return new ComplaintResponse(
                complaint.getId(),
                complaint.getSubject(),
                complaint.getDescription(),
                complaint.getCategory(),
                complaint.getStatus(),
                AuthorSummary.from(complaint.getAuthor()),
                complaint.getAdminResponse(),
                AuthorSummary.from(complaint.getHandledBy()),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                complaint.getResolvedAt(),
                revealMl ? complaint.getMlUrgency() : null,
                revealMl ? complaint.getMlPriority() : null,
                revealMl ? complaint.getMlConfidence() : null,
                revealMl ? complaint.getMlEmotion() : null,
                revealMl ? complaint.getMlEmotionConfidence() : null);
    }
}
