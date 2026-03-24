package com.example.recommender.service;

import com.example.recommender.client.EventClient;
import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.MovieStatDto;
import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationContext;
import com.example.recommender.model.RecommendationStrategy;
import com.example.recommender.repository.RecommendationStrategyRepository;
import com.example.recommender.service.algorithm.RecommendationAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int GLOBAL_EVENT_LIMIT = 5000;
    private static final Map<String, Double> DEFAULT_EVENT_WEIGHTS = Map.of(
            "VIEW_CARD", 1.0,
            "WATCH_TRAILER", 1.5,
            "FAVORITE", 4.0,
            "BOOKMARK", 2.5,
            "SHARE", 2.0,
            "START_WATCHING", 3.0,
            "FINISH_WATCHING", 5.0,
            "RATE", 4.5
    );

    private final EventClient eventClient;
    private final RecommendationStrategyRepository strategyRepository;
    private final List<RecommendationAlgorithm> algorithms;

    public RecommendationResponse recommendPopular(String period, int limit) {
        RecommendationStrategy strategy = cloneStrategy(resolveDefaultStrategy(AlgorithmType.POPULARITY));
        List<EventDto> events = fetchEvents(null, period, GLOBAL_EVENT_LIMIT);
        Map<Long, Double> popularity = toPopularityMap(eventClient.getStatsByMovie(period));
        RecommendationContext context = RecommendationContext.builder()
                .limit(limit)
                .strategy(strategy)
                .requestedAlgorithm(AlgorithmType.POPULARITY)
                .userEvents(List.of())
                .allEvents(events)
                .seenMovieIds(Set.of())
                .eventWeights(resolveWeights(strategy))
                .popularityScores(popularity)
                .focusMovieId(null)
                .build();
        return algorithm(AlgorithmType.POPULARITY).recommend(context);
    }

    public RecommendationResponse recommendTrending(String period, int limit) {
        RecommendationStrategy base = cloneStrategy(resolveDefaultStrategy(AlgorithmType.POPULARITY));
        base.setTimeDecayHalfLifeDays(7);
        List<EventDto> events = fetchEvents(null, period != null ? period : "WEEK", GLOBAL_EVENT_LIMIT);
        Map<Long, Double> popularity = toPopularityMap(eventClient.getStatsByMovie(period));
        RecommendationContext context = RecommendationContext.builder()
                .limit(limit)
                .strategy(base)
                .requestedAlgorithm(AlgorithmType.POPULARITY)
                .userEvents(List.of())
                .allEvents(events)
                .seenMovieIds(Set.of())
                .eventWeights(resolveWeights(base))
                .popularityScores(popularity)
                .focusMovieId(null)
                .build();
        return algorithm(AlgorithmType.POPULARITY).recommend(context);
    }

    public RecommendationResponse recommendSimilar(Long movieId, int limit) {
        if (movieId == null) {
            throw new IllegalArgumentException("movieId is required");
        }
        RecommendationStrategy strategy = cloneStrategy(resolveDefaultStrategy(AlgorithmType.CONTENT_BASED));
        Map<Long, Double> popularity = toPopularityMap(eventClient.getStatsByMovie("WEEK"));
        RecommendationContext context = RecommendationContext.builder()
                .limit(limit)
                .strategy(strategy)
                .requestedAlgorithm(AlgorithmType.CONTENT_BASED)
                .userEvents(List.of())
                .allEvents(List.of())
                .seenMovieIds(Set.of(movieId))
                .eventWeights(resolveWeights(strategy))
                .popularityScores(popularity)
                .focusMovieId(movieId)
                .build();
        return algorithm(AlgorithmType.CONTENT_BASED).recommend(context);
    }

    public RecommendationResponse recommendForUser(Long userId,
                                                   int limit,
                                                   String period,
                                                   AlgorithmType requestedAlgo,
                                                   Long strategyId) {
        RecommendationStrategy strategy = resolveStrategy(requestedAlgo, strategyId);
        RecommendationStrategy strategySnapshot = cloneStrategy(strategy);
        List<EventDto> userEvents = fetchEvents(userId, period, 2000);
        List<EventDto> allEvents = fetchEvents(null, period, GLOBAL_EVENT_LIMIT);
        Set<Long> seen = userEvents.stream()
                .map(EventDto::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Double> popularity = toPopularityMap(eventClient.getStatsByMovie(period));

        RecommendationContext context = RecommendationContext.builder()
                .userId(userId)
                .limit(limit)
                .strategy(strategySnapshot)
                .requestedAlgorithm(requestedAlgo)
                .userEvents(userEvents)
                .allEvents(allEvents)
                .seenMovieIds(seen)
                .eventWeights(resolveWeights(strategySnapshot))
                .popularityScores(popularity)
                .focusMovieId(null)
                .build();

        if (strategySnapshot.getMinEventsPerUser() != null
                && userEvents.size() < strategySnapshot.getMinEventsPerUser()) {
            return fallback(context, strategySnapshot);
        }

        RecommendationResponse response = algorithm(strategySnapshot.getAlgorithm()).recommend(context);
        if (response.getItems().isEmpty() && strategySnapshot.getFallbackAlgorithm() != null) {
            return fallback(context, strategySnapshot);
        }
        return response;
    }

    private RecommendationResponse fallback(RecommendationContext context, RecommendationStrategy strategy) {
        AlgorithmType fallbackAlgo = Optional.ofNullable(strategy.getFallbackAlgorithm())
                .orElse(AlgorithmType.POPULARITY);
        RecommendationStrategy fallbackStrategy = cloneStrategy(resolveDefaultStrategy(fallbackAlgo));
        RecommendationContext fallbackContext = RecommendationContext.builder()
                .userId(context.getUserId())
                .limit(context.getLimit())
                .strategy(fallbackStrategy)
                .requestedAlgorithm(fallbackAlgo)
                .userEvents(context.getUserEvents())
                .allEvents(context.getAllEvents())
                .seenMovieIds(context.getSeenMovieIds())
                .eventWeights(resolveWeights(fallbackStrategy))
                .popularityScores(context.getPopularityScores())
                .focusMovieId(context.getFocusMovieId())
                .build();
        return algorithm(fallbackAlgo).recommend(fallbackContext);
    }

    private List<EventDto> fetchEvents(Long userId, String period, int limit) {
        List<EventDto> events = eventClient.getEvents(userId, null, null, period, limit);
        return events == null ? List.of() : events;
    }

    private Map<Long, Double> toPopularityMap(List<MovieStatDto> stats) {
        if (stats == null) return Map.of();
        return stats.stream()
                .collect(Collectors.toMap(MovieStatDto::getMovieId, stat -> stat.getCount().doubleValue(), Double::sum));
    }

    private RecommendationStrategy resolveStrategy(AlgorithmType requestedAlgo, Long strategyId) {
        if (strategyId != null) {
            return strategyRepository.findById(strategyId)
                    .orElseThrow(() -> new IllegalArgumentException("Стратегия не найдена: " + strategyId));
        }
        if (requestedAlgo != null) {
            return resolveDefaultStrategy(requestedAlgo);
        }
        return strategyRepository.findByActiveTrue().stream().findFirst()
                .orElseGet(() -> resolveDefaultStrategy(AlgorithmType.HYBRID));
    }

    private RecommendationStrategy resolveDefaultStrategy(AlgorithmType algorithm) {
        return strategyRepository.findByActiveTrue().stream()
                .filter(strategy -> strategy.getAlgorithm() == algorithm)
                .findFirst()
                .orElseGet(() -> {
                    RecommendationStrategy s = new RecommendationStrategy();
                    s.setName(algorithm.name() + " default");
                    s.setAlgorithm(algorithm);
                    s.setEventWeights(DEFAULT_EVENT_WEIGHTS);
                    s.setTimeDecayHalfLifeDays(30);
                    s.setMinEventsPerUser(3);
                    s.setCandidateLimit(400);
                    s.setFallbackAlgorithm(AlgorithmType.POPULARITY);
                    s.setActive(true);
                    return strategyRepository.save(s);
                });
    }

    private RecommendationStrategy cloneStrategy(RecommendationStrategy original) {
        RecommendationStrategy clone = new RecommendationStrategy();
        clone.setId(original.getId());
        clone.setName(original.getName());
        clone.setAlgorithm(original.getAlgorithm());
        clone.setEventWeights(original.getEventWeights());
        clone.setTimeDecayHalfLifeDays(original.getTimeDecayHalfLifeDays());
        clone.setMinEventsPerUser(original.getMinEventsPerUser());
        clone.setCandidateLimit(original.getCandidateLimit());
        clone.setFallbackAlgorithm(original.getFallbackAlgorithm());
        clone.setActive(original.isActive());
        clone.setCreatedAt(original.getCreatedAt());
        clone.setUpdatedAt(original.getUpdatedAt());
        return clone;
    }

    private Map<String, Double> resolveWeights(RecommendationStrategy strategy) {
        if (strategy.getEventWeights() == null || strategy.getEventWeights().isEmpty()) {
            return DEFAULT_EVENT_WEIGHTS;
        }
        Map<String, Double> merged = new HashMap<>(DEFAULT_EVENT_WEIGHTS);
        merged.putAll(strategy.getEventWeights());
        return merged;
    }

    private RecommendationAlgorithm algorithm(AlgorithmType type) {
        return algorithms.stream()
                .filter(algo -> algo.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Алгоритм не найден: " + type));
    }
}
