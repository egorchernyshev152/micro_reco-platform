package com.example.recommender.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationAnalyticsDto {
    private String period;
    private Instant generatedAt;
    private long clicks;
    private long watchStarts;
    private long watchCompletions;
    private long ratings;
    private double conversionRate;
    private double completionRate;
    private List<RecommendationAlgorithmBreakdownDto> algorithms;
    private List<RecommendationTrendPointDto> trend;
}
