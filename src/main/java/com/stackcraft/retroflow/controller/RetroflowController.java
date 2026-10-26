package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RetroflowController {

    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;

    public RetroflowController(TeamService teamService, RetrospectiveService retrospectiveService) {
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
    }

    @PostMapping("/teams")
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    @GetMapping("/teams/{id}")
    public TeamResponse getTeam(@PathVariable Long id) {
        return teamService.getTeam(id);
    }

    @PostMapping("/teams/{teamId}/retrospectives")
    public ResponseEntity<RetrospectiveResponse> createRetrospective(
            @PathVariable Long teamId,
            @Valid @RequestBody CreateRetrospectiveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(retrospectiveService.createRetrospective(teamId, request));
    }

    @GetMapping("/teams/{teamId}/retrospectives")
    public List<RetrospectiveResponse> getRetrospectivesForTeam(@PathVariable Long teamId) {
        return retrospectiveService.getRetrospectivesForTeam(teamId);
    }

    @PutMapping("/retrospectives/{id}/close")
    public ResponseEntity<Void> closeRetrospective(@PathVariable Long id) {
        retrospectiveService.closeRetrospective(id);
        return ResponseEntity.noContent().build();
    }
}
