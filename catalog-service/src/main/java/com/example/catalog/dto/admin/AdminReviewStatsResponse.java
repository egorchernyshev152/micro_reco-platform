package com.example.catalog.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminReviewStatsResponse {
    long total;
    long pending;
    long published;
    long spam;
    long deleted;
    long flagged;
}
