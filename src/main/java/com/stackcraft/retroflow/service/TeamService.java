package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.TeamResponse;
import org.springframework.stereotype.Service;

@Service
public class TeamService {

    public TeamResponse createTeam(CreateTeamRequest request) {
        throw new UnsupportedOperationException("Team creation has not been implemented yet");
    }

    public TeamResponse getTeam(Long teamId) {
        throw new UnsupportedOperationException("Team retrieval has not been implemented yet");
    }
}
