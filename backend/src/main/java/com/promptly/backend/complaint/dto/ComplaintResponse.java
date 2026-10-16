package com.promptly.backend.complaint.dto;

import java.time.Instant;

import com.promptly.backend.complaint.Complaint;
import com.promptly.backend.complaint.ComplaintCategory;
import com.promptly.backend.complaint.ComplaintStatus;

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
        Instant resolvedAt
) {
    public static ComplaintResponse from(Complaint complaint) {
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
                complaint.getResolvedAt());
    }
}
