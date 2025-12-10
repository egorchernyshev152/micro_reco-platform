package com.example.recommender.service.algorithm;

import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationContext;

public interface RecommendationAlgorithm {
    AlgorithmType type();

    RecommendationResponse recommend(RecommendationContext context);
}
