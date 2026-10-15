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
                InstructorSummary.from(course.getInstructor()),
                course.getCreatedAt(),
                course.getUpdatedAt());
    }
}
