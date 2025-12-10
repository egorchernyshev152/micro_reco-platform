package com.example.recommender.service.algorithm;

import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationContext;
import org.springframework.stereotype.Component;

@Component
public class MlEmbeddingAlgorithm implements RecommendationAlgorithm {
    @Override
    public AlgorithmType type() {
        return AlgorithmType.ML_EMBEDDING;
    }

    @Override
    public RecommendationResponse recommend(RecommendationContext context) {
        // заглушка под внешний ML сервис, который будет вызываться позднее
        return RecommendationResponse.builder()
                .algorithm(type())
                .strategyId(context.getStrategy().getId())
                .generatedAt(java.time.Instant.now())
                .items(java.util.List.of())
                .build();
    }
}
