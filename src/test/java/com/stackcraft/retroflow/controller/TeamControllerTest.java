package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.PagingInfo;
import com.stackcraft.retroflow.dto.TeamMemberResponse;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.stackcraft.retroflow.support.ErrorAssertions.assertBusinessRule;
import static com.stackcraft.retroflow.support.ErrorAssertions.assertNotFound;
import static com.stackcraft.retroflow.support.ErrorAssertions.assertValidationError;
import static com.stackcraft.retroflow.support.JsonRequests.json;
import static com.stackcraft.retroflow.support.JsonRequests.map;
import static com.stackcraft.retroflow.support.JsonRequests.member;
import static com.stackcraft.retroflow.support.JsonRequests.validTeam;
import static com.stackcraft.retroflow.support.JsonRequests.with;
import static com.stackcraft.retroflow.support.JsonRequests.without;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

    private static TeamResponse team(long id) {
        return new TeamResponse(id, "Team-Alpha", List.of(new TeamMemberResponse(10L, "Alice", "alice@example.com")));
    }

    private ResultActions createTeam(String body) throws Exception {
        return mockMvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // ---- #1 POST /api/teams ----

    @Test
    void createTeam_returns201WithTeamBody() throws Exception {
        when(teamService.createTeam(any(CreateTeamRequest.class))).thenReturn(team(1L));

        createTeam(json(validTeam()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Team-Alpha"))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.members[0].id").value(10))
                .andExpect(jsonPath("$.members[0].name").value("Alice"))
                .andExpect(jsonPath("$.members[0].email").value("alice@example.com"));
    }

    @Test
    void createTeam_passesRequestBodyToService() throws Exception {
        when(teamService.createTeam(any(CreateTeamRequest.class))).thenReturn(team(1L));

        createTeam(json(validTeam())).andExpect(status().isCreated());

        verify(teamService).createTeam(new CreateTeamRequest("Team-Alpha",
                List.of(new com.stackcraft.retroflow.dto.CreateTeamMemberRequest("Alice", "alice@example.com"))));
    }

    static Stream<Map<String, Object>> invalidTeams() {
        List<Object> oneMember = List.of(member("Alice", "alice@example.com"));
        return Stream.of(
                with(validTeam(), "name", ""),
                with(validTeam(), "name", "   "),
                with(validTeam(), "name", null),
                without(validTeam(), "name"),
                with(validTeam(), "name", "abcd"),
                with(validTeam(), "name", "a".repeat(256)),
                with(validTeam(), "name", "Team Alpha!"),
                with(validTeam(), "members", List.of()),
                with(validTeam(), "members", null),
                without(validTeam(), "members"),
                with(validTeam(), "members", List.of(member("", "alice@example.com"))),
                with(validTeam(), "members", List.of(member("a".repeat(256), "alice@example.com"))),
                with(validTeam(), "members", List.of(member("Alice", ""))),
                with(validTeam(), "members", List.of(member("Alice", "not-an-email"))),
                with(validTeam(), "members", List.of(member("Alice", "a".repeat(250) + "@example.com"))),
                with(validTeam(), "members", List.of(oneMember.get(0), member("", "bob@example.com")))
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTeams")
    void createTeam_invalidInput_returns400ValidationError(Map<String, Object> body) throws Exception {
        assertValidationError(createTeam(json(body)));
        verifyNoInteractions(teamService);
    }

    @Test
    void createTeam_malformedJson_returns400ValidationError() throws Exception {
        assertValidationError(createTeam("{\"name\": \"Team-Alpha\", "));
        verifyNoInteractions(teamService);
    }

    @Test
    void createTeam_duplicateTeamName_returns409Conflict() throws Exception {
        when(teamService.createTeam(any(CreateTeamRequest.class)))
                .thenThrow(new RetroflowException("Team name already exists"));

        assertBusinessRule(createTeam(json(validTeam())), "Team name already exists");
    }

    @Test
    void createTeam_duplicateMemberEmail_returns409Conflict() throws Exception {
        when(teamService.createTeam(any(CreateTeamRequest.class)))
                .thenThrow(new RetroflowException("Duplicate member email"));
        Map<String, Object> body = with(validTeam(), "members",
                List.of(member("Alice", "alice@example.com"), member("Alicia", "alice@example.com")));

        assertBusinessRule(createTeam(json(body)), "Duplicate member email");
    }

    @Test
    void createTeam_serviceNotImplemented_returns501() throws Exception {
        when(teamService.createTeam(any(CreateTeamRequest.class)))
                .thenThrow(new UnsupportedOperationException("Team creation has not been implemented yet"));

        createTeam(json(validTeam()))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error").value("Team creation has not been implemented yet"))
                .andExpect(jsonPath("$.status").value(501))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ---- #2 GET /api/teams ----

    @Test
    void listTeams_returns200WithPagedBody() throws Exception {
        when(teamService.listTeams(anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(team(1L), team(2L)), new PagingInfo(42)));

        mockMvc.perform(get("/api/teams").param("pageNumber", "1").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Team-Alpha"))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.paging.totalItems").value(42));

        verify(teamService).listTeams(1, 2);
    }

    @Test
    void listTeams_withoutParams_usesDefaultPageNumber0AndPageSize20() throws Exception {
        when(teamService.listTeams(anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(), new PagingInfo(0)));

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.paging.totalItems").value(0));

        verify(teamService).listTeams(0, 20);
    }

    @Test
    void listTeams_pageSize100_isAccepted() throws Exception {
        when(teamService.listTeams(anyInt(), anyInt()))
                .thenReturn(new PagedResponse<>(List.of(), new PagingInfo(0)));

        mockMvc.perform(get("/api/teams").param("pageSize", "100")).andExpect(status().isOk());

        verify(teamService).listTeams(0, 100);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pageSize=0", "pageSize=101", "pageSize=-1", "pageNumber=-1",
            "pageNumber=abc", "pageSize=abc"})
    void listTeams_badPaging_returns400ValidationError(String query) throws Exception {
        String[] kv = query.split("=");
        assertValidationError(mockMvc.perform(get("/api/teams").param(kv[0], kv[1])));
        verify(teamService, never()).listTeams(anyInt(), anyInt());
    }

    // ---- #3 GET /api/teams/{id} ----

    @Test
    void getTeam_returns200WithBody() throws Exception {
        when(teamService.getTeam(5L)).thenReturn(team(5L));

        mockMvc.perform(get("/api/teams/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Team-Alpha"))
                .andExpect(jsonPath("$.members[0].email").value("alice@example.com"));
    }

    @Test
    void getTeam_nonNumericId_returns400ValidationError() throws Exception {
        assertValidationError(mockMvc.perform(get("/api/teams/abc")));
        verifyNoInteractions(teamService);
    }

    @Test
    void getTeam_unknownId_returns404NotFound() throws Exception {
        when(teamService.getTeam(eq(99L))).thenThrow(new NotFoundException("Team not found: 99"));

        assertNotFound(mockMvc.perform(get("/api/teams/99")), "Team not found: 99");
    }
}
