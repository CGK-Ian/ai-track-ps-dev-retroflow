package com.stackcraft.retroflow.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds raw JSON request bodies so the tests do not depend on request DTO classes. */
public final class JsonRequests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonRequests() {
    }

    public static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            result.put((String) keyValues[i], keyValues[i + 1]);
        }
        return result;
    }

    public static Map<String, Object> with(Map<String, Object> body, String key, Object value) {
        Map<String, Object> copy = new LinkedHashMap<>(body);
        copy.put(key, value);
        return copy;
    }

    public static Map<String, Object> without(Map<String, Object> body, String key) {
        Map<String, Object> copy = new LinkedHashMap<>(body);
        copy.remove(key);
        return copy;
    }

    public static Map<String, Object> member(String name, String email) {
        return map("name", name, "email", email);
    }

    public static Map<String, Object> validTeam() {
        return map("name", "Team-Alpha", "members", List.of(member("Alice", "alice@example.com")));
    }

    public static Map<String, Object> validRetrospective() {
        return map("title", "Sprint 1 Retro", "email", "alice@example.com");
    }

    public static Map<String, Object> validFeedback() {
        return map("content", "Good teamwork", "type", "WENT_WELL", "email", "alice@example.com");
    }

    public static Map<String, Object> validActionItem() {
        return map("content", "Automate deploys", "type", "ACTION_ITEM", "priority", "HIGH",
                "email", "alice@example.com");
    }

    public static Map<String, Object> validUpdate() {
        return map("content", "Updated content", "priority", "LOW", "email", "alice@example.com");
    }

    public static String json(Map<String, Object> body) {
        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
