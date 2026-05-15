package com.example.recommender.service.algorithm;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.dto.MovieDto;
import com.example.recommender.dto.RecommendationItemDto;
import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HybridAlgorithm implements RecommendationAlgorithm {

    private final ContentSimilarityService similarityService;
    private final CatalogClient catalogClient;
    private final CooccurrenceAlgorithm cooccurrenceAlgorithm;

    private static final double DEFAULT_CONTENT_WEIGHT = 0.6;
    private static final double DEFAULT_COLLABORATIVE_WEIGHT = 0.3;
    private static final double DEFAULT_POPULARITY_WEIGHT = 0.4;

    @Override
    public AlgorithmType type() {
        return AlgorithmType.HYBRID;
    }

    @Override
    public RecommendationResponse recommend(RecommendationContext context) {
        Set<Long> exclude = new HashSet<>(context.getSeenMovieIds() == null ? Set.of() : context.getSeenMovieIds());
        if (context.getFocusMovieId() != null) {
            exclude.add(context.getFocusMovieId());
        }
        int candidateLimit = Math.max(context.getLimit() * 3,
                Optional.ofNullable(context.getStrategy().getCandidateLimit()).orElse(300));
        var contentResult = similarityService.score(
                context.getSeenMovieIds() == null ? Set.of() : context.getSeenMovieIds(),
                context.getFocusMovieId(),
                exclude,
                candidateLimit);

        Map<Long, Double> contentScores = new HashMap<>();
        contentResult.items().forEach(item -> contentScores.put(item.getMovie().getId(), item.getScore()));
        Map<Long, MovieDto> movieIndex = new HashMap<>(contentResult.movieIndex());

        Map<Long, Double> popularity = Optional.ofNullable(context.getPopularityScores()).orElse(Map.of());

        Map<Long, Double> collaborativeScores = new HashMap<>();
        if (context.getUserEvents() != null && !context.getUserEvents().isEmpty()) {
            var collaborativeResponse = cooccurrenceAlgorithm.recommend(context);
            collaborativeResponse.getItems().forEach(item -> {
                if (item.getMovie() != null && item.getMovie().getId() != null) {
                    collaborativeScores.put(item.getMovie().getId(), item.getScore());
                    movieIndex.put(item.getMovie().getId(), item.getMovie());
                }
            });
        }

        Set<Long> candidateIds = new HashSet<>(contentScores.keySet());
        candidateIds.addAll(popularity.keySet());
        candidateIds.addAll(collaborativeScores.keySet());
        candidateIds.removeAll(exclude);

        double contentWeight = Optional.ofNullable(context.getStrategy().getContentWeight())
                .orElse(DEFAULT_CONTENT_WEIGHT);
        double collaborativeWeight = Optional.ofNullable(context.getStrategy().getCollaborativeWeight())
                .orElse(DEFAULT_COLLABORATIVE_WEIGHT);
        double popularityWeight = Optional.ofNullable(context.getStrategy().getPopularityWeight())
                .orElse(DEFAULT_POPULARITY_WEIGHT);
        double weightSum = contentWeight + collaborativeWeight + popularityWeight;
        if (weightSum <= 0.0) {
            contentWeight = DEFAULT_CONTENT_WEIGHT;
            collaborativeWeight = DEFAULT_COLLABORATIVE_WEIGHT;
            popularityWeight = DEFAULT_POPULARITY_WEIGHT;
            weightSum = contentWeight + collaborativeWeight + popularityWeight;
        }
        contentWeight /= weightSum;
        collaborativeWeight /= weightSum;
        popularityWeight /= weightSum;

        double maxContent = contentScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double maxCollaborative = collaborativeScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double maxPopularity = popularity.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        Set<Long> missingIds = candidateIds.stream()
                .filter(id -> !movieIndex.containsKey(id))
                .collect(Collectors.toSet());
        if (!missingIds.isEmpty()) {
            catalogClient.getMoviesByIds(missingIds).forEach(movie -> {
                if (movie.getId() != null) {
                    movieIndex.put(movie.getId(), movie);
                }
            });
        }

        final double normalizedContentWeight = contentWeight;
        final double normalizedCollaborativeWeight = collaborativeWeight;
        final double normalizedPopularityWeight = popularityWeight;

        List<RecommendationItemDto> items = candidateIds.stream()
                .map(id -> {
                    double normalizedContent = maxContent > 0
                            ? contentScores.getOrDefault(id, 0.0) / maxContent : 0.0;
                    double normalizedCollaborative = maxCollaborative > 0
                            ? collaborativeScores.getOrDefault(id, 0.0) / maxCollaborative : 0.0;
                    double normalizedPopularity = maxPopularity > 0
                            ? popularity.getOrDefault(id, 0.0) / maxPopularity : 0.0;
                    double finalScore = normalizedContentWeight * normalizedContent
                            + normalizedCollaborativeWeight * normalizedCollaborative
                            + normalizedPopularityWeight * normalizedPopularity;
                    MovieDto movie = movieIndex.get(id);
                    return movie == null ? null : RecommendationItemDto.builder()
                            .movie(movie)
                            .score(finalScore)
                            .contentScore(contentScores.getOrDefault(id, 0.0))
                            .collaborativeScore(collaborativeScores.getOrDefault(id, 0.0))
                            .popularityScore(popularity.getOrDefault(id, 0.0))
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(RecommendationItemDto::getScore).reversed())
                .limit(context.getLimit())
                .toList();

        return RecommendationResponse.builder()
                .algorithm(type())
                .strategyId(context.getStrategy().getId())
                .generatedAt(Instant.now())
                .items(items)
                .build();
    }
}
