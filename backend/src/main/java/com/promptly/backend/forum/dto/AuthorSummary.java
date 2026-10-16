package com.promptly.backend.forum.dto;

import com.promptly.backend.user.Role;
import com.promptly.backend.user.User;

public record AuthorSummary(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role
) {
    public static AuthorSummary from(User user) {
        return new AuthorSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole());
    }
}
