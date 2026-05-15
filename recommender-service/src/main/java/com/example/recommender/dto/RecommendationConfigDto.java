package com.example.recommender.dto;

import com.example.recommender.model.AlgorithmType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class RecommendationConfigDto {
    boolean enabled;
    String trainingPeriod;
    AlgorithmType defaultAlgorithm;
    Long defaultStrategyId;
    Integer recommendationLimit;
    Integer rebuildBatchSize;
    Integer maxUsersPerJob;
    Instant createdAt;
    Instant updatedAt;
    RecommendationRebuildLogDto activeRebuild;
    RecommendationRebuildLogDto lastRebuild;
}
