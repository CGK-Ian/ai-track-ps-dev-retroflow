package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateFeedbackItemRequest;
import com.stackcraft.retroflow.dto.FeedbackItemResponse;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.UpdateFeedbackItemRequest;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.service.FeedbackItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/retrospectives/{retrospectiveId}/feedbackItems")
@Validated
public class FeedbackItemController {

    private final FeedbackItemService feedbackItemService;

    public FeedbackItemController(FeedbackItemService feedbackItemService) {
        this.feedbackItemService = feedbackItemService;
    }

    @PostMapping
    public ResponseEntity<FeedbackItemResponse> createFeedbackItem(
            @PathVariable Long retrospectiveId,
            @Valid @RequestBody CreateFeedbackItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackItemService.createFeedbackItem(retrospectiveId, request));
    }

    @GetMapping
    public PagedResponse<FeedbackItemResponse> listFeedbackItems(
            @PathVariable Long retrospectiveId,
            @RequestParam(required = false) ActionPriority priority,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return feedbackItemService.listFeedbackItems(retrospectiveId, priority, completed, pageNumber, pageSize);
    }

    @GetMapping("/{feedbackItemId}")
    public FeedbackItemResponse getFeedbackItem(
            @PathVariable Long retrospectiveId,
            @PathVariable Long feedbackItemId) {
        return feedbackItemService.getFeedbackItem(retrospectiveId, feedbackItemId);
    }

    @PutMapping("/{feedbackItemId}")
    public FeedbackItemResponse updateFeedbackItem(
            @PathVariable Long retrospectiveId,
            @PathVariable Long feedbackItemId,
            @Valid @RequestBody UpdateFeedbackItemRequest request) {
        return feedbackItemService.updateFeedbackItem(retrospectiveId, feedbackItemId, request);
    }

    @DeleteMapping("/{feedbackItemId}")
    public ResponseEntity<Void> deleteFeedbackItem(
            @PathVariable Long retrospectiveId,
            @PathVariable Long feedbackItemId,
            @RequestParam @NotBlank @Email String email) {
        feedbackItemService.deleteFeedbackItem(retrospectiveId, feedbackItemId, email);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{feedbackItemId}/complete")
    public FeedbackItemResponse completeActionItem(
            @PathVariable Long retrospectiveId,
            @PathVariable Long feedbackItemId,
            @RequestParam @NotBlank @Email String email) {
        return feedbackItemService.completeActionItem(retrospectiveId, feedbackItemId, email);
    }
}