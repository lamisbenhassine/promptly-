package com.promptly.backend.course.dto;

import java.time.Instant;

import com.promptly.backend.course.Course;
import com.promptly.backend.course.CourseCategory;

public record CourseResponse(
        Long id,
        String title,
        String description,
        String learningObjectives,
        CourseCategory category,
        boolean published,
        String imageUrl,
        String pdfUrl,
        InstructorSummary instructor,
        Instant createdAt,
        Instant updatedAt
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getLearningObjectives(),
                course.getCategory(),
                course.isPublished(),
                toPublicUrl(course.getImagePath()),
                toPublicUrl(course.getPdfPath()),
                InstructorSummary.from(course.getInstructor()),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }

    private static String toPublicUrl(String relativePath) {
        return relativePath == null || relativePath.isBlank()
                ? null
                : "/api/files/" + relativePath;
    }
}
