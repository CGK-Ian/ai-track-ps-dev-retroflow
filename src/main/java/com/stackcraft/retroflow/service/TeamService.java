package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Team createTeam(String name, List<String> members) {
        if (name == null || name.isBlank()) {
            throw new RetroflowException("Team name is required");
        }
        if (members == null || members.isEmpty()) {
            throw new RetroflowException("A team must have at least one member");
        }

        Team team = new Team();
        team.setName(name);
        team.setMembers(List.copyOf(members));
        return teamRepository.save(team);
    }

    public TeamResponse createTeam(CreateTeamRequest request) {
        throw new UnsupportedOperationException("Team creation has not been implemented yet");
    }

    public TeamResponse getTeam(Long teamId) {
        throw new UnsupportedOperationException("Team retrieval has not been implemented yet");
    }
}