package com.stackcraft.retroflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTeamRequest(
        @NotBlank
        @Size(min = 5, max = 255)
        @Pattern(regexp = "^[A-Za-z0-9-]+$")
        String name,
        @NotEmpty
        List<@Valid CreateTeamMemberRequest> members
) {
}
