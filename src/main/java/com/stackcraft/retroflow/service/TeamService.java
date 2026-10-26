package com.stackcraft.retroflow.service;

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
}