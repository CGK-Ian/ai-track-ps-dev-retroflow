package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Team createTeam(String name, List<String> members) {
        if (members == null || members.isEmpty()) {
            throw new RetroflowException("A team must have at least one member");
        }

        Team team = new Team();
        team.setName(name);
        team.setMembers(List.copyOf(members));
        return teamRepository.save(team);
    }
}