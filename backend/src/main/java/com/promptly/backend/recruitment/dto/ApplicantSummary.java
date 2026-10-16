package com.promptly.backend.recruitment.dto;

import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;

public record ApplicantSummary(
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role
) {
    public static ApplicantSummary from(User user) {
        if (user == null) {
            return null;
        }
        return new ApplicantSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole());
    }
}
