package com.promptly.backend.recruitment.dto;

import com.promptly.backend.recruitment.ApplicationStatus;

import jakarta.validation.constraints.NotNull;

public record ApplicationStatusRequest(
        @NotNull ApplicationStatus status
) {
}
