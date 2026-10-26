package com.stackcraft.retroflow.support;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Asserts the single {@code ErrorMessage{code, description}} format. */
public final class ErrorAssertions {

    private ErrorAssertions() {
    }

    public static void assertError(ResultActions result, HttpStatus status, String code) throws Exception {
        result.andExpect(status().is(status.value()))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.description").value(not(emptyOrNullString())));
    }

    public static void assertValidationError(ResultActions result) throws Exception {
        assertError(result, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    public static void assertBusinessRule(ResultActions result, String message) throws Exception {
        assertError(result, HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION");
        result.andExpect(jsonPath("$.description").value(containsString(message)));
    }

    public static void assertNotFound(ResultActions result, String message) throws Exception {
        assertError(result, HttpStatus.NOT_FOUND, "NOT_FOUND");
        result.andExpect(jsonPath("$.description").value(containsString(message)));
    }
}
