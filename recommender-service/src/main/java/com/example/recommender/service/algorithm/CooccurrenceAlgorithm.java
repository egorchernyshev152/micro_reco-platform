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
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CooccurrenceAlgorithm implements RecommendationAlgorithm {

    private final CatalogClient catalogClient;

    @Override
    public AlgorithmType type() {
        return AlgorithmType.CO_OCCURRENCE;
    }

    @Override
    public RecommendationResponse recommend(RecommendationContext context) {
        List<EventDto> allEvents = context.getAllEvents() == null ? List.of() : context.getAllEvents();
        if (context.getUserEvents() == null || context.getUserEvents().isEmpty() || allEvents.isEmpty()) {
            return emptyResponse(context);
        }

        Map<String, Double> weights = context.getEventWeights();
        int halfLifeDays = Optional.ofNullable(context.getStrategy().getTimeDecayHalfLifeDays()).orElse(30);
        double lambda = Math.log(2) / Math.max(1, halfLifeDays);
        Instant now = Instant.now();

        Map<Long, Map<Long, Double>> userItemWeights = new HashMap<>();
        for (EventDto event : allEvents) {
            if (event.getUserId() == null || event.getMovieId() == null || event.getCreatedAt() == null) continue;
            double weight = weights.getOrDefault(event.getType(), 1.0);
            long ageDays = Math.max(0, Duration.between(event.getCreatedAt(), now).toDays());
            double decay = Math.exp(-lambda * ageDays);
            double score = weight * decay;
            userItemWeights.computeIfAbsent(event.getUserId(), k -> new HashMap<>())
                    .merge(event.getMovieId(), score, Double::sum);
        }

        Map<Long, Map<Long, Double>> similarity = new HashMap<>();
        for (Map<Long, Double> items : userItemWeights.values()) {
            List<Long> ids = new ArrayList<>(items.keySet());
            for (int i = 0; i < ids.size(); i++) {
                Long movieA = ids.get(i);
                for (int j = 0; j < ids.size(); j++) {
                    if (i == j) continue;
                    Long movieB = ids.get(j);
                    double contrib = Math.sqrt(items.get(movieA) * items.get(movieB));
                    similarity.computeIfAbsent(movieA, k -> new HashMap<>())
                            .merge(movieB, contrib, Double::sum);
                }
            }
        }

        Map<Long, Double> candidateScores = new HashMap<>();
        Set<Long> seen = Optional.ofNullable(context.getSeenMovieIds()).orElse(Set.of());
        for (Long seenMovie : seen) {
            Map<Long, Double> sims = similarity.getOrDefault(seenMovie, Collections.emptyMap());
            for (Map.Entry<Long, Double> entry : sims.entrySet()) {
                if (seen.contains(entry.getKey())) continue;
                candidateScores.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        if (candidateScores.isEmpty()) {
            return emptyResponse(context);
        }

        int limit = Math.min(context.getLimit(),
                Optional.ofNullable(context.getStrategy().getCandidateLimit()).orElse(200));
        List<Long> topIds = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        List<MovieDto> movies = catalogClient.getMoviesByIds(topIds);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < topIds.size(); i++) {
            order.put(topIds.get(i), i);
        }

        List<RecommendationItemDto> result = movies.stream()
                .sorted(Comparator.comparingInt(m -> order.getOrDefault(m.getId(), Integer.MAX_VALUE)))
                .map(movie -> RecommendationItemDto.builder()
                        .movie(movie)
                        .score(candidateScores.getOrDefault(movie.getId(), 0.0))
                        .build())
                .collect(Collectors.toList());

        return RecommendationResponse.builder()
                .algorithm(type())
                .strategyId(context.getStrategy().getId())
                .generatedAt(now)
                .items(result)
                .build();
    }

    private RecommendationResponse emptyResponse(RecommendationContext context) {
        return RecommendationResponse.builder()
                .algorithm(type())
                .strategyId(context.getStrategy().getId())
                .generatedAt(Instant.now())
                .items(List.of())
                .build();
    }
}
