package com.example.recommender.dto;

import com.example.recommender.model.AlgorithmType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class StrategyCreateRequest {
    @NotBlank
    private String name;
    @NotNull
    private AlgorithmType algorithm;
    private Map<String, Double> eventWeights;
    private Integer timeDecayHalfLifeDays;
    private Integer minEventsPerUser;
    private Integer candidateLimit;
    private AlgorithmType fallbackAlgorithm;
    private Boolean isActive;
}
