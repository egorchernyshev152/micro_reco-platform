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
public class ActivityAnalyticsDto {
    private String period;
    private Instant generatedAt;
    private long activeUsers;
    private double avgEventsPerUser;
    private List<ActivitySegmentDto> segments;
    private List<DailyMetricPointDto> trend;
    private List<HourlyMetricPointDto> hourlyDistribution;
}
