package com.stackcraft.retroflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedbackItemResponse(
        Long id,
        Long retrospectiveId,
        String content,
        FeedbackType type,
        TeamMemberResponse submittedBy,
        ActionPriority priority,
        Boolean completed
) {
}