package com.example.recommender.dto;

import com.example.recommender.model.AlgorithmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StrategyResponse {
    private Long id;
    private String name;
    private AlgorithmType algorithm;
    private Map<String, Double> eventWeights;
    private Integer timeDecayHalfLifeDays;
    private Integer minEventsPerUser;
    private Integer candidateLimit;
    private AlgorithmType fallbackAlgorithm;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
