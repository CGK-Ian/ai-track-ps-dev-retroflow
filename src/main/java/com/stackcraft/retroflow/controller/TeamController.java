package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
@Validated
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    @GetMapping
    public PagedResponse<TeamResponse> listTeams(
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        return teamService.listTeams(pageNumber, pageSize);
    }

    @GetMapping("/{id}")
    public TeamResponse getTeam(@PathVariable Long id) {
        return teamService.getTeam(id);
    }
}