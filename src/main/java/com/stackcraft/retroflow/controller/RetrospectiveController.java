package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.service.RetrospectiveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Validated
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;

    public RetrospectiveController(RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
    }

    @PostMapping("/teams/{teamId}/retrospectives")
    public ResponseEntity<RetrospectiveResponse> createRetrospective(
            @PathVariable Long teamId,
            @Valid @RequestBody CreateRetrospectiveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(retrospectiveService.createRetrospective(teamId, request));
    }

    @GetMapping("/teams/{teamId}/retrospectives")
    public PagedResponse<RetrospectiveResponse> getRetrospectivesForTeam(
            @PathVariable Long teamId,
            @RequestParam @NotBlank @Email String email,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return retrospectiveService.getRetrospectivesForTeam(teamId, email, pageNumber, pageSize);
    }

    @GetMapping("/retrospectives")
    public PagedResponse<RetrospectiveResponse> queryRetrospectives(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) RetrospectiveStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return retrospectiveService.queryRetrospectives(id, title, status, pageNumber, pageSize);
    }

    @GetMapping("/retrospectives/{id}")
    public RetrospectiveResponse getRetrospective(@PathVariable Long id) {
        return retrospectiveService.getRetrospective(id);
    }

    @PutMapping("/retrospectives/{id}/close")
    public ResponseEntity<Void> closeRetrospective(
            @PathVariable Long id,
            @RequestParam @NotBlank @Email String email) {
        retrospectiveService.closeRetrospective(id, email);
        return ResponseEntity.noContent().build();
    }
}