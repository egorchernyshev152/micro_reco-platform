package com.example.catalog.dto.admin;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class AdminReviewPageResponse {
    List<AdminReviewResponse> items;
    int page;
    int size;
    long totalElements;
    int totalPages;
    boolean hasNext;
    AdminReviewStatsResponse stats;
}
