package com.stackcraft.retroflow.support;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Asserts the shared error, status, and timestamp response format. */
public final class ErrorAssertions {

    private ErrorAssertions() {
    }

    public static void assertError(ResultActions result, HttpStatus status) throws Exception {
        result.andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.status").value(status.value()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    public static void assertValidationError(ResultActions result) throws Exception {
        assertError(result, HttpStatus.BAD_REQUEST);
    }

    public static void assertBusinessRule(ResultActions result, String message) throws Exception {
        assertError(result, HttpStatus.CONFLICT);
        result.andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString(message)));
    }

    public static void assertNotFound(ResultActions result, String message) throws Exception {
        assertError(result, HttpStatus.NOT_FOUND);
        result.andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString(message)));
    }
}
