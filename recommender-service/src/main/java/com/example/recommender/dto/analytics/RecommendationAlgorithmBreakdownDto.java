package com.example.recommender.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationAlgorithmBreakdownDto {
    private String algorithm;
    private long views;
    private long starts;
    private long finishes;
    private long ratings;
}
