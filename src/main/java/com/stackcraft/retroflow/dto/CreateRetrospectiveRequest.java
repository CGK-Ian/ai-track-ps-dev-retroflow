package com.stackcraft.retroflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRetrospectiveRequest(
        @NotBlank @Size(max = 255) String title,
        LocalDate date,
        @Pattern(regexp = "(?i)OPEN|CLOSED", message = "status must be OPEN or CLOSED") String status
) {
}
