package com.promptly.backend.chapter.dto;

import java.time.Instant;

import com.promptly.backend.chapter.Chapter;

public record ChapterResponse(
        Long id,
        Long courseId,
        String title,
        String content,
        String codeExample,
        int position,
        String pdfUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChapterResponse from(Chapter chapter) {
        String pdf = chapter.getPdfPath();
        return new ChapterResponse(
                chapter.getId(),
                chapter.getCourse().getId(),
                chapter.getTitle(),
                chapter.getContent(),
                chapter.getCodeExample(),
                chapter.getPosition(),
                pdf == null || pdf.isBlank() ? null : "/api/files/" + pdf,
                chapter.getCreatedAt(),
                chapter.getUpdatedAt());
    }
}
