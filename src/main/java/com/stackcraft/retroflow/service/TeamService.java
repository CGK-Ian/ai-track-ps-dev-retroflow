package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.dto.CreateTeamMemberRequest;
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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        if (teamRepository.existsByName(request.name())) {
            throw new RetroflowException("Team name already exists");
        }
        Set<String> emails = new HashSet<>();
        Team team = new Team();
        team.setName(request.name());
        for (CreateTeamMemberRequest memberRequest : request.members()) {
            String email = memberRequest.email();
            if (!emails.add(email.toLowerCase()) || teamRepository.existsByTeamMembers_EmailIgnoreCase(email)) {
                throw new RetroflowException("Duplicate member email: " + email);
            }
            team.addTeamMember(new TeamMember(null, memberRequest.name(), email, team));
        }
        return toResponse(teamRepository.save(team));
    }

    @Transactional(readOnly = true)
    public PagedResponse<TeamResponse> listTeams(int pageNumber, int pageSize) {
        Page<Team> page = teamRepository.findAll(PageRequest.of(pageNumber, pageSize, Sort.by("id")));
        return new PagedResponse<>(page.map(TeamService::toResponse).getContent(),
            new PagingInfo((int) page.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId) {
        return toResponse(teamRepository.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team not found: " + teamId)));
    }

    static TeamResponse toResponse(Team team) {
        List<TeamMemberResponse> members = team.getTeamMembers().stream()
            .map(member -> new TeamMemberResponse(member.getId(), member.getName(), member.getEmail()))
            .toList();
        return new TeamResponse(team.getId(), team.getName(), members);
    }
}