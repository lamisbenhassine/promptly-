package com.promptly.backend.complaint.dto;

import com.promptly.backend.complaint.ComplaintStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComplaintStatusRequest(
        @NotNull ComplaintStatus status,
        @Size(max = 5000) String adminResponse
) {
}
