package com.example.recommender.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PopularMovieDto {
    private Long movieId;
    private String title;
    private String posterUrl;
    private long events;
    private double share;
    private Map<String, Long> eventTypes;
}
