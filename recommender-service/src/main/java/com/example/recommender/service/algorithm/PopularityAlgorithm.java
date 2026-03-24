package com.example.recommender.service.algorithm;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.MovieDto;
import com.example.recommender.dto.RecommendationItemDto;
import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PopularityAlgorithm implements RecommendationAlgorithm {

    private final CatalogClient catalogClient;

    @Override
    public AlgorithmType type() {
        return AlgorithmType.POPULARITY;
    }

    @Override
    public RecommendationResponse recommend(RecommendationContext context) {
        Map<Long, Double> scores = new HashMap<>();
        Map<String, Double> weights = context.getEventWeights();
        int halfLifeDays = Optional.ofNullable(context.getStrategy().getTimeDecayHalfLifeDays()).orElse(30);
        double lambda = Math.log(2) / Math.max(1, halfLifeDays);
        Instant now = Instant.now();

        for (EventDto event : context.getAllEvents()) {
            if (event.getMovieId() == null || event.getCreatedAt() == null) {
                continue;
            }
            double weight = weights.getOrDefault(event.getType(), 1.0);
            if ("RATE".equalsIgnoreCase(event.getType()) && event.getPayload() != null) {
                Object scorePayload = event.getPayload().get("score");
                if (scorePayload instanceof Number number) {
                    weight = Math.max(weight, number.doubleValue());
                }
            }
            long ageDays = Math.max(0, Duration.between(event.getCreatedAt(), now).toDays());
            double decay = Math.exp(-lambda * ageDays);
            scores.merge(event.getMovieId(), weight * decay, Double::sum);
        }

        if (scores.isEmpty() && context.getPopularityScores() != null) {
            context.getPopularityScores().forEach((id, value) -> scores.merge(id, value, Double::sum));
        }

        List<Long> topIds = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(context.getLimit())
                .map(Map.Entry::getKey)
                .toList();

        List<MovieDto> movies;
        if (topIds.isEmpty()) {
            movies = catalogClient.getAllMovies().stream()
                    .limit(context.getLimit())
                    .toList();
        } else {
            movies = catalogClient.getMoviesByIds(topIds);
        }

        if (movies.isEmpty()) {
            return RecommendationResponse.builder()
                    .algorithm(type())
                    .strategyId(context.getStrategy().getId())
                    .generatedAt(now)
                    .items(List.of())
                    .build();
        }

        Map<Long, Integer> order = new HashMap<>();
        List<Long> orderingSource = topIds.isEmpty()
                ? movies.stream().map(MovieDto::getId).toList()
                : topIds;
        for (int i = 0; i < orderingSource.size(); i++) {
            order.put(orderingSource.get(i), i);
        }

        List<RecommendationItemDto> result = movies.stream()
                .sorted(Comparator.comparingInt(m -> order.getOrDefault(m.getId(), Integer.MAX_VALUE)))
                .map(movie -> RecommendationItemDto.builder()
                        .movie(movie)
                        .score(scores.getOrDefault(movie.getId(), 0.0))
                        .popularityScore(scores.getOrDefault(movie.getId(), 0.0))
                        .build())
                .toList();

        return RecommendationResponse.builder()
                .algorithm(type())
                .strategyId(context.getStrategy().getId())
                .generatedAt(now)
                .items(result)
                .build();
    }
}
