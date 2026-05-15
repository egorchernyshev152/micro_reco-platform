package com.example.recommender.dto;

import com.example.recommender.model.RecommendationRebuildStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class RecommendationRebuildLogDto {
    Long id;
    RecommendationRebuildStatus status;
    Integer processedUsers;
    Integer totalUsers;
    Instant startedAt;
    Instant finishedAt;
    String initiator;
    String trainingPeriod;
    String message;
}
