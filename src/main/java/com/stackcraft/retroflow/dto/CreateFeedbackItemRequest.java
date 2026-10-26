package com.stackcraft.retroflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFeedbackItemRequest(
        @NotBlank String content,
        @NotNull FeedbackType type,
        @NotBlank @Email String email,
        ActionPriority priority
) {

    @JsonIgnore
    @AssertTrue(message = "priority is required for an ACTION_ITEM")
    public boolean isPriorityPresentForActionItem() {
        return type != FeedbackType.ACTION_ITEM || priority != null;
    }
}