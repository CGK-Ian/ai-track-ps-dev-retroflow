package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.ActionPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateFeedbackItemRequest(
        @NotBlank String content,
        ActionPriority priority,
        @NotBlank @Email String email
) {
}