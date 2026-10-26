package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.PagingInfo;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.RetrospectiveService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.stackcraft.retroflow.support.ErrorAssertions.assertBusinessRule;
import static com.stackcraft.retroflow.support.ErrorAssertions.assertNotFound;
import static com.stackcraft.retroflow.support.ErrorAssertions.assertValidationError;
import static com.stackcraft.retroflow.support.JsonRequests.json;
import static com.stackcraft.retroflow.support.JsonRequests.validRetrospective;
import static com.stackcraft.retroflow.support.JsonRequests.with;
import static com.stackcraft.retroflow.support.JsonRequests.without;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RetrospectiveController.class)
class RetrospectiveControllerTest {

    private static final String EMAIL = "alice@example.com";
    private static final LocalDate TODAY = LocalDate.now();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RetrospectiveService retrospectiveService;

    private static RetrospectiveResponse retro(long id, RetrospectiveStatus status) {
        return new RetrospectiveResponse(id, "Sprint 1 Retro", TODAY, status, 7L);
    }

    private static PagedResponse<RetrospectiveResponse> page(int total, RetrospectiveResponse... items) {
        return new PagedResponse<>(List.of(items), new PagingInfo(total));
    }

    private ResultActions createRetro(String teamId, String body) throws Exception {
        return mockMvc.perform(post("/api/teams/" + teamId + "/retrospectives")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // ---- #4 POST /api/teams/{teamId}/retrospectives ----

    @Test
    void createRetrospective_returns201WithBody() throws Exception {
        when(retrospectiveService.createRetrospective(eq(7L), any(CreateRetrospectiveRequest.class)))
                .thenReturn(retro(1L, RetrospectiveStatus.OPEN));

        createRetro("7", json(validRetrospective()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Sprint 1 Retro"))
                .andExpect(jsonPath("$.date").value(TODAY.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.teamId").value(7));
    }

    @Test
    void createRetrospective_withExplicitDate_passesRequestToService() throws Exception {
        when(retrospectiveService.createRetrospective(eq(7L), any(CreateRetrospectiveRequest.class)))
                .thenReturn(retro(1L, RetrospectiveStatus.OPEN));

        createRetro("7", json(with(validRetrospective(), "date", "2026-01-31")))
                .andExpect(status().isCreated());

        verify(retrospectiveService).createRetrospective(eq(7L), any(CreateRetrospectiveRequest.class));
    }

    @Test
    void createRetrospective_statusInRequestIsIgnored_responseIsAlwaysOpen() throws Exception {
        when(retrospectiveService.createRetrospective(eq(7L), any(CreateRetrospectiveRequest.class)))
                .thenReturn(retro(1L, RetrospectiveStatus.OPEN));

        createRetro("7", json(with(validRetrospective(), "status", "CLOSED")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    static Stream<Map<String, Object>> invalidRetrospectives() {
        return Stream.of(
                with(validRetrospective(), "title", ""),
                with(validRetrospective(), "title", "  "),
                without(validRetrospective(), "title"),
                with(validRetrospective(), "title", "a".repeat(256)),
                without(validRetrospective(), "email"),
                with(validRetrospective(), "email", ""),
                with(validRetrospective(), "email", "not-an-email"),
                with(validRetrospective(), "date", "not-a-date")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidRetrospectives")
    void createRetrospective_invalidInput_returns400ValidationError(Map<String, Object> body) throws Exception {
        assertValidationError(createRetro("7", json(body)));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void createRetrospective_malformedJson_returns400ValidationError() throws Exception {
        assertValidationError(createRetro("7", "{\"title\": "));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void createRetrospective_nonNumericTeamId_returns400ValidationError() throws Exception {
        assertValidationError(createRetro("abc", json(validRetrospective())));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void createRetrospective_unknownTeam_returns404() throws Exception {
        when(retrospectiveService.createRetrospective(eq(99L), any(CreateRetrospectiveRequest.class)))
                .thenThrow(new NotFoundException("Team not found: 99"));

        assertNotFound(createRetro("99", json(validRetrospective())), "Team not found: 99");
    }

    @Test
    void createRetrospective_secondOpenRetrospective_returns409Conflict() throws Exception {
        when(retrospectiveService.createRetrospective(eq(7L), any(CreateRetrospectiveRequest.class)))
                .thenThrow(new RetroflowException("Team already has an open retrospective"));

        assertBusinessRule(createRetro("7", json(validRetrospective())),
                "Team already has an open retrospective");
    }

    @Test
    void createRetrospective_emailNotOnRoster_returns409Conflict() throws Exception {
        when(retrospectiveService.createRetrospective(eq(7L), any(CreateRetrospectiveRequest.class)))
                .thenThrow(new RetroflowException("Email is not a member of the team"));

        assertBusinessRule(createRetro("7", json(validRetrospective())), "not a member of the team");
    }

    // ---- #5 GET /api/teams/{teamId}/retrospectives ----

    @Test
    void listRetrospectivesForTeam_returns200WithPagedBody() throws Exception {
        when(retrospectiveService.getRetrospectivesForTeam(eq(7L), eq(EMAIL), anyInt(), anyInt()))
                .thenReturn(page(3, retro(1L, RetrospectiveStatus.OPEN), retro(2L, RetrospectiveStatus.CLOSED)));

        mockMvc.perform(get("/api/teams/7/retrospectives")
                        .param("email", EMAIL).param("pageNumber", "1").param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].status").value("OPEN"))
                .andExpect(jsonPath("$.items[1].status").value("CLOSED"))
                .andExpect(jsonPath("$.paging.totalItems").value(3));

        verify(retrospectiveService).getRetrospectivesForTeam(7L, EMAIL, 1, 2);
    }

    @Test
    void listRetrospectivesForTeam_usesDefaultPaging() throws Exception {
        when(retrospectiveService.getRetrospectivesForTeam(anyLong(), anyString(), anyInt(), anyInt()))
                .thenReturn(page(0));

        mockMvc.perform(get("/api/teams/7/retrospectives").param("email", EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.paging.totalItems").value(0));

        verify(retrospectiveService).getRetrospectivesForTeam(7L, EMAIL, 0, 20);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pageSize=0", "pageSize=101", "pageNumber=-1", "pageNumber=abc"})
    void listRetrospectivesForTeam_badPaging_returns400(String query) throws Exception {
        String[] kv = query.split("=");
        assertValidationError(mockMvc.perform(get("/api/teams/7/retrospectives")
                .param("email", EMAIL).param(kv[0], kv[1])));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void listRetrospectivesForTeam_missingEmail_returns400() throws Exception {
        assertValidationError(mockMvc.perform(get("/api/teams/7/retrospectives")));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void listRetrospectivesForTeam_unknownTeam_returns404() throws Exception {
        when(retrospectiveService.getRetrospectivesForTeam(eq(99L), anyString(), anyInt(), anyInt()))
                .thenThrow(new NotFoundException("Team not found: 99"));

        assertNotFound(mockMvc.perform(get("/api/teams/99/retrospectives").param("email", EMAIL)),
                "Team not found: 99");
    }

    @Test
    void listRetrospectivesForTeam_emailNotOnRoster_returns409Conflict() throws Exception {
        when(retrospectiveService.getRetrospectivesForTeam(anyLong(), anyString(), anyInt(), anyInt()))
                .thenThrow(new RetroflowException("Email is not a member of the team"));

        assertBusinessRule(mockMvc.perform(get("/api/teams/7/retrospectives").param("email", EMAIL)),
                "not a member of the team");
    }

    // ---- #6 GET /api/retrospectives?id=&title=&status= ----

    @Test
    void queryRetrospectives_byId_returns200() throws Exception {
        when(retrospectiveService.queryRetrospectives(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(1, retro(5L, RetrospectiveStatus.OPEN)));

        mockMvc.perform(get("/api/retrospectives").param("id", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(5))
                .andExpect(jsonPath("$.paging.totalItems").value(1));

        verify(retrospectiveService).queryRetrospectives(eq(5L), eq(null), eq(null), eq(0), eq(20));
    }

    @Test
    void queryRetrospectives_byTitle_passesTitleToService() throws Exception {
        when(retrospectiveService.queryRetrospectives(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(1, retro(5L, RetrospectiveStatus.OPEN)));

        mockMvc.perform(get("/api/retrospectives").param("title", "sprint"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Sprint 1 Retro"));

        verify(retrospectiveService).queryRetrospectives(eq(null), eq("sprint"), eq(null), eq(0), eq(20));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "open", "Closed"})
    void queryRetrospectives_byStatus_isCaseInsensitive(String status) throws Exception {
        when(retrospectiveService.queryRetrospectives(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(1, retro(5L, RetrospectiveStatus.OPEN)));

        mockMvc.perform(get("/api/retrospectives").param("status", status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void queryRetrospectives_noFilters_returns200() throws Exception {
        when(retrospectiveService.queryRetrospectives(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(0));

        mockMvc.perform(get("/api/retrospectives"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paging.totalItems").value(0));
    }

    @Test
    void queryRetrospectives_badStatus_returns400() throws Exception {
        assertValidationError(mockMvc.perform(get("/api/retrospectives").param("status", "ARCHIVED")));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void queryRetrospectives_nonNumericId_returns400() throws Exception {
        assertValidationError(mockMvc.perform(get("/api/retrospectives").param("id", "abc")));
        verifyNoInteractions(retrospectiveService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pageSize=0", "pageSize=101", "pageNumber=-1", "pageNumber=abc"})
    void queryRetrospectives_badPaging_returns400(String query) throws Exception {
        String[] kv = query.split("=");
        assertValidationError(mockMvc.perform(get("/api/retrospectives").param(kv[0], kv[1])));
        verifyNoInteractions(retrospectiveService);
    }

    // ---- #7 GET /api/retrospectives/{id} ----

    @Test
    void getRetrospective_returns200WithBody() throws Exception {
        when(retrospectiveService.getRetrospective(5L)).thenReturn(retro(5L, RetrospectiveStatus.CLOSED));

        mockMvc.perform(get("/api/retrospectives/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.title").value("Sprint 1 Retro"))
                .andExpect(jsonPath("$.date").value(TODAY.toString()))
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.teamId").value(7));
    }

    @Test
    void getRetrospective_nonNumericId_returns400() throws Exception {
        assertValidationError(mockMvc.perform(get("/api/retrospectives/abc")));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void getRetrospective_unknownId_returns404() throws Exception {
        when(retrospectiveService.getRetrospective(99L))
                .thenThrow(new NotFoundException("Retrospective not found: 99"));

        assertNotFound(mockMvc.perform(get("/api/retrospectives/99")), "Retrospective not found: 99");
    }

    // ---- #8 PUT /api/retrospectives/{id}/close ----

    @Test
    void closeRetrospective_returns204WithEmptyBody() throws Exception {
        mockMvc.perform(put("/api/retrospectives/5/close").param("email", EMAIL))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(retrospectiveService).closeRetrospective(5L, EMAIL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-an-email"})
    void closeRetrospective_invalidEmail_returns400(String email) throws Exception {
        assertValidationError(mockMvc.perform(put("/api/retrospectives/5/close").param("email", email)));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void closeRetrospective_missingEmail_returns400() throws Exception {
        assertValidationError(mockMvc.perform(put("/api/retrospectives/5/close")));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void closeRetrospective_unknownId_returns404() throws Exception {
        doThrow(new NotFoundException("Retrospective not found: 99"))
                .when(retrospectiveService).closeRetrospective(99L, EMAIL);

        assertNotFound(mockMvc.perform(put("/api/retrospectives/99/close").param("email", EMAIL)),
                "Retrospective not found: 99");
    }

    @Test
    void closeRetrospective_alreadyClosed_returns409Conflict() throws Exception {
        doThrow(new RetroflowException("Retrospective is already closed"))
                .when(retrospectiveService).closeRetrospective(5L, EMAIL);

        assertBusinessRule(mockMvc.perform(put("/api/retrospectives/5/close").param("email", EMAIL)),
                "already closed");
    }

    @Test
    void closeRetrospective_emailNotOnRoster_returns409Conflict() throws Exception {
        doThrow(new RetroflowException("Email is not a member of the team"))
                .when(retrospectiveService).closeRetrospective(5L, EMAIL);

        assertBusinessRule(mockMvc.perform(put("/api/retrospectives/5/close").param("email", EMAIL)),
                "not a member of the team");
    }
}
