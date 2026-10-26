package com.stackcraft.retroflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRetrospectiveRequest(
        @NotBlank @Size(max = 255) String title,
        LocalDate date,
        @NotBlank @Email String email
) {
}