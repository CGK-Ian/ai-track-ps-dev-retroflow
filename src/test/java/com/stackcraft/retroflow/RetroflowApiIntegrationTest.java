package com.stackcraft.retroflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Controllers wired to the real services and the in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class RetroflowApiIntegrationTest {

    private static final String ALICE = "alice@example.com";

    @Autowired
    private MockMvc mockMvc;

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String body) throws Exception {
        return mockMvc.perform(body == null ? request : request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void fullFlow() throws Exception {
        send(post("/api/teams"), """
            {"name":"Team-Flow","members":[{"name":"Alice","email":"alice@example.com"}]}""")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.members[0].email").value(ALICE));
        send(post("/api/teams"), """
            {"name":"Team-Flow","members":[{"name":"Bob","email":"bob@example.com"}]}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
        send(post("/api/teams"), """
            {"name":"Team-Other","members":[{"name":"Al","email":"ALICE@example.com"}]}""")
            .andExpect(status().isBadRequest());
        send(get("/api/teams"), null).andExpect(status().isOk()).andExpect(jsonPath("$.paging.totalItems").value(1));
        send(get("/api/teams/999"), null).andExpect(status().isNotFound());

        send(post("/api/teams/1/retrospectives"), """
            {"title":"Sprint 1","email":"alice@example.com"}""")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.date").isNotEmpty());
        send(post("/api/teams/1/retrospectives"), """
            {"title":"Sprint 2","email":"alice@example.com"}""")
            .andExpect(status().isBadRequest());
        send(get("/api/teams/1/retrospectives?email=" + ALICE), null)
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        send(get("/api/teams/1/retrospectives?email=nobody@example.com"), null).andExpect(status().isBadRequest());
        send(get("/api/retrospectives?title=sprint&status=open"), null)
            .andExpect(status().isOk()).andExpect(jsonPath("$.paging.totalItems").value(1));
        send(get("/api/retrospectives"), null).andExpect(jsonPath("$.paging.totalItems").value(1));
        send(get("/api/retrospectives?id=1"), null).andExpect(jsonPath("$.paging.totalItems").value(1));
        send(get("/api/retrospectives?title=zzz"), null).andExpect(jsonPath("$.paging.totalItems").value(0));
        send(get("/api/retrospectives/1"), null).andExpect(jsonPath("$.teamId").value(1));

        String base = "/api/retrospectives/1/feedbackItems";
        send(post(base), """
            {"content":"Good","type":"went_well","email":"alice@example.com"}""")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.submittedBy.email").value(ALICE))
            .andExpect(jsonPath("$.completed").doesNotExist());
        send(post(base), """
            {"content":"Fix CI","type":"ACTION_ITEM","priority":"high","email":"alice@example.com"}""")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.completed").value(false));
        send(get(base), null).andExpect(jsonPath("$.paging.totalItems").value(2));
        send(get(base + "?completed=false"), null).andExpect(jsonPath("$.paging.totalItems").value(1));
        send(get(base + "?priority=high&completed=false"), null).andExpect(jsonPath("$.paging.totalItems").value(1));
        send(get(base + "?priority=low"), null).andExpect(jsonPath("$.paging.totalItems").value(0));
        send(get(base + "/2"), null).andExpect(status().isOk());
        send(put(base + "/2"), """
            {"content":"Fix CI now","priority":"LOW","email":"alice@example.com"}""")
            .andExpect(jsonPath("$.priority").value("LOW"));

        send(put("/api/retrospectives/1/close?email=" + ALICE), null).andExpect(status().isNoContent());
        send(put("/api/retrospectives/1/close?email=" + ALICE), null).andExpect(status().isBadRequest());
        send(delete(base + "/1?email=" + ALICE), null).andExpect(status().isBadRequest());
        send(put(base + "/2/complete?email=" + ALICE), null)
            .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        send(put(base + "/2/complete?email=" + ALICE), null).andExpect(status().isBadRequest());
        send(put(base + "/2"), """
            {"content":"x","email":"alice@example.com"}""").andExpect(status().isBadRequest());
        send(post(base), """
            {"content":"Late","type":"WENT_WELL","email":"alice@example.com"}""")
            .andExpect(status().isBadRequest());
    }
}
