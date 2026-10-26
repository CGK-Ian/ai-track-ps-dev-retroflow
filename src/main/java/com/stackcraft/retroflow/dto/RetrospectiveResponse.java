package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.RetrospectiveStatus;

import java.time.LocalDate;

public record RetrospectiveResponse(
        Long id,
        String title,
        LocalDate date,
        RetrospectiveStatus status,
        Long teamId
) {
}
