package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.ActionPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFeedbackItemRequest(
        @NotBlank @Size(max = 5000) String content,
        ActionPriority priority,
        @NotBlank @Email String email
) {
}