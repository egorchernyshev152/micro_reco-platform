package com.example.recommender.dto;

import com.example.recommender.model.AlgorithmType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecommendationConfigUpdateRequest {
    @NotBlank
    private String trainingPeriod;
    private AlgorithmType defaultAlgorithm;
    private Long defaultStrategyId;
    @Min(1)
    @Max(100)
    private Integer recommendationLimit;
    @Min(1)
    private Integer rebuildBatchSize;
    @Min(1)
    private Integer maxUsersPerJob;
    private Boolean enabled;
}
