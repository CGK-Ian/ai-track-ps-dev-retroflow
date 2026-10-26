package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.PagingInfo;
import com.stackcraft.retroflow.dto.TeamMemberResponse;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.entity.TeamMember;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public Team createTeam(String name, List<String> members) {
        validateTeamName(name);
        validateMemberNames(members);
        if (teamRepository.existsByName(name)) {
            throw new RetroflowException("Team new: name already exists");
        }

        Team team = new Team();
        team.setName(name);
        team.setMembers(List.copyOf(members));
        return teamRepository.save(team);
    }

    public TeamResponse createTeam(CreateTeamRequest request) {
        validateTeamName(request.name());
        if (teamRepository.existsByName(request.name())) {
            throw new RetroflowException("Team new: name already exists");
        }

        Set<String> submittedEmails = new HashSet<>();
        for (var memberRequest : request.members()) {
            if (!submittedEmails.add(memberRequest.email().toLowerCase())) {
                throw new RetroflowException("Team new: duplicate member email");
            }
        }
        boolean existingEmail = teamRepository.findAll().stream()
                .flatMap(team -> team.getTeamMembers().stream())
                .anyMatch(member -> submittedEmails.contains(member.getEmail().toLowerCase()));
        if (existingEmail) {
            throw new RetroflowException("Team new: duplicate member email");
        }

        Team team = new Team();
        team.setName(request.name());
        team.setMembers(new ArrayList<>(request.members().stream().map(member -> member.name()).toList()));
        team.setTeamMembers(new ArrayList<>(request.members().stream()
                .map(member -> new TeamMember(null, member.name(), member.email(), team))
                .toList()));
        return toResponse(teamRepository.save(team));
    }

    @Transactional(readOnly = true)
    public PagedResponse<TeamResponse> listTeams(int pageNumber, int pageSize) {
        Page<Team> page = teamRepository.findAll(PageRequest.of(pageNumber, pageSize));
        return new PagedResponse<>(page.map(this::toResponse).getContent(),
                new PagingInfo(Math.toIntExact(page.getTotalElements())));
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Team " + teamId + ": not found"));
        return toResponse(team);
    }

    private void validateTeamName(String name) {
        if (name == null || name.isBlank()) {
            throw new RetroflowException("Team new: name is required");
        }
        if (name.length() < 5 || name.length() > 255 || !name.matches("[A-Za-z0-9-]+")) {
            throw new RetroflowException("Team new: name must be 5 to 255 letters, digits, or dashes");
        }
    }

    private void validateMemberNames(List<String> members) {
        if (members == null || members.isEmpty()) {
            throw new RetroflowException("Team new: must have at least one member");
        }
        if (members.stream().anyMatch(name -> name == null || name.isBlank() || name.length() > 255)) {
            throw new RetroflowException("Team new: member names must be nonblank and at most 255 characters");
        }
    }

    private TeamResponse toResponse(Team team) {
        List<TeamMemberResponse> members = team.getTeamMembers().stream()
                .map(member -> new TeamMemberResponse(member.getId(), member.getName(), member.getEmail()))
                .toList();
        return new TeamResponse(team.getId(), team.getName(), members);
    }
}
