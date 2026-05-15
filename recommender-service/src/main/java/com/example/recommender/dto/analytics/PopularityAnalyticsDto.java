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
public class PopularityAnalyticsDto {
    private String period;
    private Instant generatedAt;
    private List<PopularMovieDto> topMovies;
    private List<PopularityTrendPointDto> trend;
}
