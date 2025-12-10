package com.example.recommender.dto;

import com.example.recommender.model.AlgorithmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {
    private AlgorithmType algorithm;
    private Long strategyId;
    private Instant generatedAt;
    private List<RecommendationItemDto> items;
}
