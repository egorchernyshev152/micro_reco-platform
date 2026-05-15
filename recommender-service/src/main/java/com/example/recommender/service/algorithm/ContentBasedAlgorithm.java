package com.example.recommender.service.algorithm;

import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ContentBasedAlgorithm implements RecommendationAlgorithm {

    private final ContentSimilarityService similarityService;

    @Override
    public AlgorithmType type() {
        return AlgorithmType.CONTENT_BASED;
    }

    @Override
    public RecommendationResponse recommend(RecommendationContext context) {
        Set<Long> seen = new HashSet<>(context.getSeenMovieIds() == null ? Set.of() : context.getSeenMovieIds());
        if (context.getFocusMovieId() != null) {
            seen.add(context.getFocusMovieId());
        }
        var result = similarityService.score(
                context.getSeenMovieIds() == null ? Set.of() : context.getSeenMovieIds(),
                context.getFocusMovieId(),
                seen,
                context.getLimit());
        return RecommendationResponse.builder()
                .algorithm(type())
                .strategyId(context.getStrategy().getId())
                .generatedAt(Instant.now())
                .items(result.items())
                .build();
    }
}
