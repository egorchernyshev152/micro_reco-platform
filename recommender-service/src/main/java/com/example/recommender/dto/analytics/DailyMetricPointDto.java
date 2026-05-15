package com.example.recommender.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMetricPointDto {
    private String day;
    private long events;
    private Long activeUsers;
}
