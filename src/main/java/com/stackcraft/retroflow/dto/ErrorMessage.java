package com.stackcraft.retroflow.dto;

import java.time.Instant;

public record ErrorMessage(String error, int status, Instant timestamp) {
}
