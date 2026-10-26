package com.stackcraft.retroflow.dto;

import java.util.List;

public record PagedResponse<T>(List<T> items, PagingInfo paging) {
}