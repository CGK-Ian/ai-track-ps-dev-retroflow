package com.stackcraft.retroflow.dto;

import java.util.List;

public record TeamResponse(Long id, String name, List<TeamMemberResponse> members) {
}
