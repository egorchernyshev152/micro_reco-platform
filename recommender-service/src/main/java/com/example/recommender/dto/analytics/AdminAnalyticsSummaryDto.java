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
public class AdminAnalyticsSummaryDto {
    private String period;
    private Instant generatedAt;
    private long totalEvents;
    private long activeUsers;
    private double avgEventsPerUser;
    private long recommendationClicks;
    private long recommendationStarts;
    private double recommendationConversion;
    private List<DailyMetricPointDto> trend;
}
