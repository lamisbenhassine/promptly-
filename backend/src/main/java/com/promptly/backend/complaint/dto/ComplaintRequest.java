package com.promptly.backend.complaint.dto;

import com.promptly.backend.complaint.ComplaintCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComplaintRequest(
        @NotBlank @Size(min = 5, max = 180) String subject,
        @NotBlank @Size(min = 10, max = 5000) String description,
        @NotNull ComplaintCategory category
) {
}
