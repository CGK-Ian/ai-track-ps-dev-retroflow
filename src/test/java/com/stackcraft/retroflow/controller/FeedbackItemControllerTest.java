package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateFeedbackItemRequest;
import com.stackcraft.retroflow.dto.FeedbackItemResponse;
import com.stackcraft.retroflow.dto.PagedResponse;
import com.stackcraft.retroflow.dto.PagingInfo;
import com.stackcraft.retroflow.dto.TeamMemberResponse;
import com.stackcraft.retroflow.dto.UpdateFeedbackItemRequest;
import com.stackcraft.retroflow.entity.ActionPriority;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.FeedbackItemService;
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
import static com.stackcraft.retroflow.support.JsonRequests.validActionItem;
import static com.stackcraft.retroflow.support.JsonRequests.validFeedback;
import static com.stackcraft.retroflow.support.JsonRequests.validUpdate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackItemController.class)
class FeedbackItemControllerTest {

    private static final String EMAIL = "alice@example.com";
    private static final String BASE = "/api/retrospectives/5/feedbackItems";
    private static final TeamMemberResponse ALICE = new TeamMemberResponse(10L, "Alice", EMAIL);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackItemService feedbackItemService;

    private static FeedbackItemResponse feedback(long id) {
        return new FeedbackItemResponse(id, 5L, "Good teamwork", FeedbackType.WENT_WELL, ALICE, null, null);
    }

    private static FeedbackItemResponse action(long id, boolean completed) {
        return new FeedbackItemResponse(id, 5L, "Automate deploys", FeedbackType.ACTION_ITEM, ALICE,
                ActionPriority.HIGH, completed);
    }

    private static PagedResponse<FeedbackItemResponse> page(int total, FeedbackItemResponse... items) {
        return new PagedResponse<>(List.of(items), new PagingInfo(total));
    }

    private ResultActions postJson(String url, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private ResultActions putJson(String url, Map<String, Object> body) throws Exception {
        return mockMvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    // ---- #9 POST /api/retrospectives/{retrospectiveId}/feedbackItems ----

    @Test
    void createFeedbackItem_returns201WithBody() throws Exception {
        when(feedbackItemService.createFeedbackItem(eq(5L), any(CreateFeedbackItemRequest.class)))
                .thenReturn(feedback(1L));

        postJson(BASE, validFeedback())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.retrospectiveId").value(5))
                .andExpect(jsonPath("$.content").value("Good teamwork"))
                .andExpect(jsonPath("$.type").value("WENT_WELL"))
                .andExpect(jsonPath("$.submittedBy.id").value(10))
                .andExpect(jsonPath("$.submittedBy.name").value("Alice"))
                .andExpect(jsonPath("$.submittedBy.email").value(EMAIL))
                .andExpect(jsonPath("$.priority").doesNotExist())
                .andExpect(jsonPath("$.completed").doesNotExist());
    }

    @Test
    void createActionItem_returns201WithPriorityAndCompletedFalse() throws Exception {
        when(feedbackItemService.createFeedbackItem(eq(5L), any(CreateFeedbackItemRequest.class)))
                .thenReturn(action(2L, false));

        postJson(BASE, validActionItem())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.type").value("ACTION_ITEM"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"went_well", "Needs_Improvement", "action_item"})
    void createFeedbackItem_typeIsCaseInsensitive(String type) throws Exception {
        when(feedbackItemService.createFeedbackItem(eq(5L), any(CreateFeedbackItemRequest.class)))
                .thenReturn(action(2L, false));

        postJson(BASE, with(validActionItem(), "type", type)).andExpect(status().isCreated());
    }

    @Test
    void createActionItem_priorityIsCaseInsensitive() throws Exception {
        when(feedbackItemService.createFeedbackItem(eq(5L), any(CreateFeedbackItemRequest.class)))
                .thenReturn(action(2L, false));

        postJson(BASE, with(validActionItem(), "priority", "high")).andExpect(status().isCreated());
    }

    static Stream<Map<String, Object>> invalidFeedback() {
        return Stream.of(
                with(validFeedback(), "content", ""),
                with(validFeedback(), "content", "  "),
                without(validFeedback(), "content"),
                without(validFeedback(), "type"),
                with(validFeedback(), "type", "NOT_A_TYPE"),
                without(validActionItem(), "priority"),
                with(validActionItem(), "priority", null),
                with(validActionItem(), "priority", "URGENT"),
                without(validFeedback(), "email"),
                with(validFeedback(), "email", ""),
                with(validFeedback(), "email", "not-an-email")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidFeedback")
    void createFeedbackItem_invalidInput_returns400(Map<String, Object> body) throws Exception {
        assertValidationError(postJson(BASE, body));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void createFeedbackItem_malformedJson_returns400() throws Exception {
        assertValidationError(mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\": ")));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void createFeedbackItem_nonNumericRetrospectiveId_returns400() throws Exception {
        assertValidationError(postJson("/api/retrospectives/abc/feedbackItems", validFeedback()));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void createFeedbackItem_unknownRetrospective_returns404() throws Exception {
        when(feedbackItemService.createFeedbackItem(eq(99L), any(CreateFeedbackItemRequest.class)))
                .thenThrow(new NotFoundException("Retrospective not found: 99"));

        assertNotFound(postJson("/api/retrospectives/99/feedbackItems", validFeedback()),
                "Retrospective not found: 99");
    }

    @Test
    void createFeedbackItem_closedRetrospective_returns409Conflict() throws Exception {
        when(feedbackItemService.createFeedbackItem(eq(5L), any(CreateFeedbackItemRequest.class)))
                .thenThrow(new RetroflowException("Cannot modify a closed retrospective"));

        assertBusinessRule(postJson(BASE, validFeedback()), "closed retrospective");
    }

    @Test
    void createFeedbackItem_submitterNotOnRoster_returns409Conflict() throws Exception {
        when(feedbackItemService.createFeedbackItem(eq(5L), any(CreateFeedbackItemRequest.class)))
                .thenThrow(new RetroflowException("Email is not a member of the team"));

        assertBusinessRule(postJson(BASE, validFeedback()), "not a member of the team");
    }

    // ---- #10 GET /api/retrospectives/{retrospectiveId}/feedbackItems ----

    @Test
    void listFeedbackItems_returns200WithPagedBody() throws Exception {
        when(feedbackItemService.listFeedbackItems(eq(5L), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(2, feedback(1L), action(2L, true)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].type").value("WENT_WELL"))
                .andExpect(jsonPath("$.items[1].type").value("ACTION_ITEM"))
                .andExpect(jsonPath("$.items[1].completed").value(true))
                .andExpect(jsonPath("$.paging.totalItems").value(2));

        verify(feedbackItemService).listFeedbackItems(5L, null, null, 0, 20);
    }

    @Test
    void listFeedbackItems_filterByPriority_returns200() throws Exception {
        when(feedbackItemService.listFeedbackItems(eq(5L), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(1, action(2L, false)));

        mockMvc.perform(get(BASE).param("priority", "high"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].priority").value("HIGH"));

        verify(feedbackItemService).listFeedbackItems(eq(5L), eq(ActionPriority.HIGH), eq(null), eq(0), eq(20));
    }

    @Test
    void listFeedbackItems_filterByCompleted_returns200() throws Exception {
        when(feedbackItemService.listFeedbackItems(eq(5L), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(1, action(2L, true)));

        mockMvc.perform(get(BASE).param("completed", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].completed").value(true));

        verify(feedbackItemService).listFeedbackItems(eq(5L), eq(null), eq(true), eq(0), eq(20));
    }

    @Test
    void listFeedbackItems_bothFilters_areCombined() throws Exception {
        when(feedbackItemService.listFeedbackItems(eq(5L), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(1, action(2L, false)));

        mockMvc.perform(get(BASE).param("priority", "HIGH").param("completed", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        verify(feedbackItemService).listFeedbackItems(eq(5L), eq(ActionPriority.HIGH), eq(false), eq(0), eq(20));
    }

    @Test
    void listFeedbackItems_paging_isPassedToService() throws Exception {
        when(feedbackItemService.listFeedbackItems(eq(5L), any(), any(), anyInt(), anyInt()))
                .thenReturn(page(30));

        mockMvc.perform(get(BASE).param("pageNumber", "2").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paging.totalItems").value(30));

        verify(feedbackItemService).listFeedbackItems(5L, null, null, 2, 10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"priority=URGENT", "completed=maybe", "pageSize=0", "pageSize=101",
            "pageNumber=-1", "pageNumber=abc"})
    void listFeedbackItems_badParams_returns400(String query) throws Exception {
        String[] kv = query.split("=");
        assertValidationError(mockMvc.perform(get(BASE).param(kv[0], kv[1])));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void listFeedbackItems_unknownRetrospective_returns404() throws Exception {
        when(feedbackItemService.listFeedbackItems(eq(99L), any(), any(), anyInt(), anyInt()))
                .thenThrow(new NotFoundException("Retrospective not found: 99"));

        assertNotFound(mockMvc.perform(get("/api/retrospectives/99/feedbackItems")),
                "Retrospective not found: 99");
    }

    // ---- #11 GET /api/retrospectives/{retrospectiveId}/feedbackItems/{feedbackItemId} ----

    @Test
    void getFeedbackItem_returns200WithBody() throws Exception {
        when(feedbackItemService.getFeedbackItem(5L, 2L)).thenReturn(action(2L, false));

        mockMvc.perform(get(BASE + "/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.retrospectiveId").value(5))
                .andExpect(jsonPath("$.content").value("Automate deploys"))
                .andExpect(jsonPath("$.type").value("ACTION_ITEM"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.submittedBy.email").value(EMAIL));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/retrospectives/abc/feedbackItems/2", "/api/retrospectives/5/feedbackItems/abc"})
    void getFeedbackItem_nonNumericIds_returns400(String url) throws Exception {
        assertValidationError(mockMvc.perform(get(url)));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void getFeedbackItem_unknownRetrospective_returns404() throws Exception {
        when(feedbackItemService.getFeedbackItem(99L, 2L))
                .thenThrow(new NotFoundException("Retrospective not found: 99"));

        assertNotFound(mockMvc.perform(get("/api/retrospectives/99/feedbackItems/2")),
                "Retrospective not found: 99");
    }

    @Test
    void getFeedbackItem_unknownItem_returns404() throws Exception {
        when(feedbackItemService.getFeedbackItem(5L, 99L))
                .thenThrow(new NotFoundException("Feedback item not found: 99"));

        assertNotFound(mockMvc.perform(get(BASE + "/99")), "Feedback item not found: 99");
    }

    @Test
    void getFeedbackItem_itemBelongsToOtherRetrospective_returns404() throws Exception {
        when(feedbackItemService.getFeedbackItem(5L, 3L))
                .thenThrow(new NotFoundException("Feedback item 3 not found in retrospective 5"));

        assertNotFound(mockMvc.perform(get(BASE + "/3")), "not found in retrospective 5");
    }

    // ---- #12 PUT /api/retrospectives/{retrospectiveId}/feedbackItems/{feedbackItemId} ----

    @Test
    void updateFeedbackItem_returns200WithUpdatedBody() throws Exception {
        when(feedbackItemService.updateFeedbackItem(eq(5L), eq(2L), any(UpdateFeedbackItemRequest.class)))
                .thenReturn(new FeedbackItemResponse(2L, 5L, "Updated content", FeedbackType.ACTION_ITEM, ALICE,
                        ActionPriority.LOW, false));

        putJson(BASE + "/2", validUpdate())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.type").value("ACTION_ITEM"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void updateFeedbackItem_priorityIsOptional() throws Exception {
        when(feedbackItemService.updateFeedbackItem(eq(5L), eq(1L), any(UpdateFeedbackItemRequest.class)))
                .thenReturn(feedback(1L));

        putJson(BASE + "/1", without(validUpdate(), "priority")).andExpect(status().isOk());
    }

    static Stream<Map<String, Object>> invalidUpdates() {
        return Stream.of(
                with(validUpdate(), "content", ""),
                with(validUpdate(), "content", "  "),
                without(validUpdate(), "content"),
                with(validUpdate(), "priority", "URGENT"),
                without(validUpdate(), "email"),
                with(validUpdate(), "email", ""),
                with(validUpdate(), "email", "not-an-email")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidUpdates")
    void updateFeedbackItem_invalidInput_returns400(Map<String, Object> body) throws Exception {
        assertValidationError(putJson(BASE + "/2", body));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void updateFeedbackItem_unknownRetrospective_returns404() throws Exception {
        when(feedbackItemService.updateFeedbackItem(eq(99L), anyLong(), any(UpdateFeedbackItemRequest.class)))
                .thenThrow(new NotFoundException("Retrospective not found: 99"));

        assertNotFound(putJson("/api/retrospectives/99/feedbackItems/2", validUpdate()),
                "Retrospective not found: 99");
    }

    @Test
    void updateFeedbackItem_unknownItem_returns404() throws Exception {
        when(feedbackItemService.updateFeedbackItem(eq(5L), eq(99L), any(UpdateFeedbackItemRequest.class)))
                .thenThrow(new NotFoundException("Feedback item not found: 99"));

        assertNotFound(putJson(BASE + "/99", validUpdate()), "Feedback item not found: 99");
    }

    @Test
    void updateFeedbackItem_completedActionItem_returns409Conflict() throws Exception {
        when(feedbackItemService.updateFeedbackItem(eq(5L), eq(2L), any(UpdateFeedbackItemRequest.class)))
                .thenThrow(new RetroflowException("A completed action item cannot be edited"));

        assertBusinessRule(putJson(BASE + "/2", validUpdate()), "completed action item cannot be edited");
    }

    @Test
    void updateFeedbackItem_emailNotOnRoster_returns409Conflict() throws Exception {
        when(feedbackItemService.updateFeedbackItem(eq(5L), eq(2L), any(UpdateFeedbackItemRequest.class)))
                .thenThrow(new RetroflowException("Email is not a member of the team"));

        assertBusinessRule(putJson(BASE + "/2", validUpdate()), "not a member of the team");
    }

    @Test
    void updateFeedbackItem_nonActionItemOnClosedRetrospective_returns409Conflict() throws Exception {
        when(feedbackItemService.updateFeedbackItem(eq(5L), eq(1L), any(UpdateFeedbackItemRequest.class)))
                .thenThrow(new RetroflowException("Cannot modify a closed retrospective"));

        assertBusinessRule(putJson(BASE + "/1", validUpdate()), "closed retrospective");
    }

    @Test
    void updateFeedbackItem_incompleteActionItemOnClosedRetrospective_returns200() throws Exception {
        when(feedbackItemService.updateFeedbackItem(eq(5L), eq(2L), any(UpdateFeedbackItemRequest.class)))
                .thenReturn(action(2L, false));

        putJson(BASE + "/2", validUpdate())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false));
    }

    // ---- #13 DELETE /api/retrospectives/{retrospectiveId}/feedbackItems/{feedbackItemId} ----

    @Test
    void deleteFeedbackItem_returns204WithEmptyBody() throws Exception {
        mockMvc.perform(delete(BASE + "/2").param("email", EMAIL))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(feedbackItemService).deleteFeedbackItem(5L, 2L, EMAIL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-an-email"})
    void deleteFeedbackItem_invalidEmail_returns400(String email) throws Exception {
        assertValidationError(mockMvc.perform(delete(BASE + "/2").param("email", email)));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void deleteFeedbackItem_missingEmail_returns400() throws Exception {
        assertValidationError(mockMvc.perform(delete(BASE + "/2")));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void deleteFeedbackItem_unknownRetrospective_returns404() throws Exception {
        doThrow(new NotFoundException("Retrospective not found: 99"))
                .when(feedbackItemService).deleteFeedbackItem(99L, 2L, EMAIL);

        assertNotFound(mockMvc.perform(delete("/api/retrospectives/99/feedbackItems/2").param("email", EMAIL)),
                "Retrospective not found: 99");
    }

    @Test
    void deleteFeedbackItem_unknownItem_returns404() throws Exception {
        doThrow(new NotFoundException("Feedback item not found: 99"))
                .when(feedbackItemService).deleteFeedbackItem(5L, 99L, EMAIL);

        assertNotFound(mockMvc.perform(delete(BASE + "/99").param("email", EMAIL)),
                "Feedback item not found: 99");
    }

    @Test
    void deleteFeedbackItem_closedRetrospective_returns409Conflict() throws Exception {
        doThrow(new RetroflowException("Cannot modify a closed retrospective"))
                .when(feedbackItemService).deleteFeedbackItem(5L, 2L, EMAIL);

        assertBusinessRule(mockMvc.perform(delete(BASE + "/2").param("email", EMAIL)), "closed retrospective");
    }

    // ---- #14 PUT .../feedbackItems/{feedbackItemId}/complete ----

    @Test
    void completeActionItem_returns200WithCompletedTrue() throws Exception {
        when(feedbackItemService.completeActionItem(5L, 2L, EMAIL)).thenReturn(action(2L, true));

        mockMvc.perform(put(BASE + "/2/complete").param("email", EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.type").value("ACTION_ITEM"))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void completeActionItem_missingEmail_returns400() throws Exception {
        assertValidationError(mockMvc.perform(put(BASE + "/2/complete")));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void completeActionItem_unknownRetrospective_returns404() throws Exception {
        when(feedbackItemService.completeActionItem(99L, 2L, EMAIL))
                .thenThrow(new NotFoundException("Retrospective not found: 99"));

        assertNotFound(mockMvc.perform(put("/api/retrospectives/99/feedbackItems/2/complete")
                .param("email", EMAIL)), "Retrospective not found: 99");
    }

    @Test
    void completeActionItem_unknownItem_returns404() throws Exception {
        when(feedbackItemService.completeActionItem(5L, 99L, EMAIL))
                .thenThrow(new NotFoundException("Feedback item not found: 99"));

        assertNotFound(mockMvc.perform(put(BASE + "/99/complete").param("email", EMAIL)),
                "Feedback item not found: 99");
    }

    @Test
    void completeActionItem_notAnActionItem_returns409Conflict() throws Exception {
        when(feedbackItemService.completeActionItem(5L, 1L, EMAIL))
                .thenThrow(new RetroflowException("Only action items can be completed"));

        assertBusinessRule(mockMvc.perform(put(BASE + "/1/complete").param("email", EMAIL)),
                "Only action items");
    }

    @Test
    void completeActionItem_alreadyCompleted_returns409Conflict() throws Exception {
        when(feedbackItemService.completeActionItem(5L, 2L, EMAIL))
                .thenThrow(new RetroflowException("Action item is already completed"));

        assertBusinessRule(mockMvc.perform(put(BASE + "/2/complete").param("email", EMAIL)),
                "already completed");
    }
}
