package com.example.recommender.service;

import com.example.recommender.model.RecommendationRebuildStatus;

public record RebuildProgressEvent(Long logId,
                                   int processed,
                                   int total,
                                   RecommendationRebuildStatus status) {
}
