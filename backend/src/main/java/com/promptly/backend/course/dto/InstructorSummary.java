package com.promptly.backend.course.dto;

import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;

/** Resume de l'enseignant rattache a un cours, sans le mot de passe. */
public record InstructorSummary(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role
) {

    public static InstructorSummary from(User user) {
        return new InstructorSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole());
    }
}
