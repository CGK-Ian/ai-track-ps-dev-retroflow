package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetrospectiveService {

    public RetrospectiveResponse createRetrospective(Long teamId, CreateRetrospectiveRequest request) {
        throw new UnsupportedOperationException("Retrospective creation has not been implemented yet");
    }

    public List<RetrospectiveResponse> getRetrospectivesForTeam(Long teamId) {
        throw new UnsupportedOperationException("Retrospective retrieval has not been implemented yet");
    }

    public void closeRetrospective(Long retrospectiveId) {
        throw new UnsupportedOperationException("Retrospective closure has not been implemented yet");
    }
}
