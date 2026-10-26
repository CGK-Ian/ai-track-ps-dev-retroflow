package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(teamRepository);
    }

    @Test
    void createTeam_withoutAuthentication_persistsTheRequestedTeamAndMembers() {
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            team.setId(17L);
            return team;
        });

        Team created = teamService.createTeam("Team-Alpha", List.of("Alice", "Bob"));

        assertThat(created.getId()).isEqualTo(17L);
        assertThat(created.getName()).isEqualTo("Team-Alpha");
        assertThat(created.getMembers()).containsExactly("Alice", "Bob");

        ArgumentCaptor<Team> savedTeam = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(savedTeam.capture());
        assertThat(savedTeam.getValue())
            .usingRecursiveComparison()
            .isEqualTo(created);
    }

    @Test
    void createTeam_rejectsMissingOrEmptyMembers() {
        assertThatThrownBy(() -> teamService.createTeam("Team-Alpha", null))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("at least one member");
        assertThatThrownBy(() -> teamService.createTeam("Team-Alpha", List.of()))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("at least one member");

        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_rejectsBlankNames() {
        assertThatThrownBy(() -> teamService.createTeam("", List.of("Alice")))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("name is required");
        assertThatThrownBy(() -> teamService.createTeam(" \t ", List.of("Alice")))
            .isInstanceOf(RetroflowException.class)
            .hasMessageContaining("name is required");

        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_rejectsNamesShorterThanFiveCharacters() {
        assertThatThrownBy(() -> teamService.createTeam("Team", List.of("Alice")))
            .isInstanceOf(RetroflowException.class);

        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_rejectsNamesLongerThan255Characters() {
        assertThatThrownBy(() -> teamService.createTeam("A".repeat(256), List.of("Alice")))
            .isInstanceOf(RetroflowException.class);

        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_rejectsNamesWithCharactersOtherThanLettersDigitsOrDash() {
        assertThatThrownBy(() -> teamService.createTeam("Team_Alpha", List.of("Alice")))
            .isInstanceOf(RetroflowException.class);

        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_acceptsNamesAtBothLengthBoundaries() {
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String maximumLengthName = "A".repeat(255);

        Team minimumLengthTeam = teamService.createTeam("Team1", List.of("Alice"));
        Team maximumLengthTeam = teamService.createTeam(maximumLengthName, List.of("Bob"));

        assertThat(minimumLengthTeam.getName()).hasSize(5).isEqualTo("Team1");
        assertThat(maximumLengthTeam.getName()).hasSize(255).isEqualTo(maximumLengthName);
    }

    @Test
    void createTeam_rejectsDuplicateTeamNames() {
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        teamService.createTeam("Team-Alpha", List.of("Alice"));

        assertThatThrownBy(() -> teamService.createTeam("Team-Alpha", List.of("Bob")))
            .isInstanceOf(RetroflowException.class);
    }

    @Test
    void createTeam_allowsDuplicateMemberNames() {
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Team created = teamService.createTeam("Team-Alpha", List.of("Alice", "Alice"));

        assertThat(created.getMembers()).containsExactly("Alice", "Alice");
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void createTeam_rejectsBlankMemberNames() {
        assertThatThrownBy(() -> teamService.createTeam("Team-Alpha", List.of(" ")))
            .isInstanceOf(RetroflowException.class);

        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void createTeam_rejectsMemberNamesLongerThan255Characters() {
        assertThatThrownBy(() -> teamService.createTeam("Team-Alpha", List.of("A".repeat(256))))
            .isInstanceOf(RetroflowException.class);

        verify(teamRepository, never()).save(any(Team.class));
    }
}
