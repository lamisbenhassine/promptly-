package com.promptly.backend.recruitment.dto;

import java.time.LocalDate;

import com.promptly.backend.recruitment.ContractType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JobOfferRequest(

        @NotBlank @Size(min = 5, max = 180)
        String title,

        @NotBlank @Size(min = 20, max = 8000)
        String description,

        @NotBlank @Size(min = 2, max = 2000)
        String skills,

        @Size(max = 2000)
        String experience,

        @Size(max = 120)
        String location,

        @NotNull
        ContractType contractType,

        boolean published,

        LocalDate deadline
) {
}
