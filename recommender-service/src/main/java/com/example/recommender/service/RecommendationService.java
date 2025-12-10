package com.example.recommender.service;

import com.example.recommender.client.EventClient;
import com.example.recommender.dto.EventDto;
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

    private final EventClient eventClient;
    private final RecommendationStrategyRepository strategyRepository;
    private final List<RecommendationAlgorithm> algorithms;

    private static final Map<String, Double> DEFAULT_EVENT_WEIGHTS = Map.of(
            "VIEW", 1.0,
            "LIKE", 3.0,
            "SAVE", 4.0
    );

    public RecommendationResponse recommendPopular(String period, int limit) {
        RecommendationStrategy strategy = resolveDefaultStrategy(AlgorithmType.POPULARITY);
        // тянем все события из event-service с учетом окна period (REST вызов)
        List<EventDto> allEvents = eventClient.getEvents(null, period);
        RecommendationContext context = RecommendationContext.builder()
                .limit(limit)
                .strategy(strategy)
                .requestedAlgorithm(AlgorithmType.POPULARITY)
                .userEvents(List.of())
                .allEvents(allEvents)
                .seenItemIds(Set.of())
                .eventWeights(resolveWeights(strategy))
                .build();
        return algorithm(AlgorithmType.POPULARITY).recommend(context);
    }

    public RecommendationResponse recommendForUser(Long userId, int limit, String period, AlgorithmType requestedAlgo, Long strategyId) {
        RecommendationStrategy strategy = resolveStrategy(requestedAlgo, strategyId);
        // получаем события пользователя и общий поток (REST к event-service) для расчета популярности/похожести
        List<EventDto> userEvents = eventClient.getEvents(userId, period);
        List<EventDto> allEvents = eventClient.getEvents(null, period);

        // собираем историю пользователя чтобы не рекомендовать уже просмотренное
        Set<Long> seen = userEvents.stream().map(EventDto::getItemId).collect(Collectors.toSet());
        RecommendationContext context = RecommendationContext.builder()
                .userId(userId)
                .limit(limit)
                .strategy(strategy)
                .requestedAlgorithm(requestedAlgo)
                .userEvents(userEvents)
                .allEvents(allEvents)
                .seenItemIds(seen)
                .eventWeights(resolveWeights(strategy))
                .build();

        if (strategy.getMinEventsPerUser() != null && userEvents.size() < strategy.getMinEventsPerUser()) {
            return fallback(context, strategy);
        }

        // пробуем выбранный алгоритм и при пустом результате откатываемся на fallback
        RecommendationResponse response = algorithm(strategy.getAlgorithm()).recommend(context);
        if (response.getItems().isEmpty() && strategy.getFallbackAlgorithm() != null) {
            return fallback(context, strategy);
        }
        return response;
    }

    private RecommendationResponse fallback(RecommendationContext context, RecommendationStrategy strategy) {
        AlgorithmType fb = Optional.ofNullable(strategy.getFallbackAlgorithm()).orElse(AlgorithmType.POPULARITY);
        RecommendationStrategy fallbackStrategy = resolveDefaultStrategy(fb);
        RecommendationContext fbContext = RecommendationContext.builder()
                .userId(context.getUserId())
                .limit(context.getLimit())
                .strategy(fallbackStrategy)
                .requestedAlgorithm(fb)
                .userEvents(context.getUserEvents())
                .allEvents(context.getAllEvents())
                .seenItemIds(context.getSeenItemIds())
                .eventWeights(resolveWeights(fallbackStrategy))
                .build();
        return algorithm(fb).recommend(fbContext);
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
                .orElseGet(() -> resolveDefaultStrategy(AlgorithmType.CO_OCCURRENCE));
    }

    private RecommendationStrategy resolveDefaultStrategy(AlgorithmType algo) {
        return strategyRepository.findByActiveTrue().stream()
                .filter(s -> s.getAlgorithm() == algo)
                .findFirst()
                .orElseGet(() -> {
                    RecommendationStrategy s = new RecommendationStrategy();
                    s.setName(algo.name() + " default");
                    s.setAlgorithm(algo);
                    s.setEventWeights(DEFAULT_EVENT_WEIGHTS);
                    s.setTimeDecayHalfLifeDays(30);
                    s.setMinEventsPerUser(3);
                    s.setCandidateLimit(200);
                    s.setFallbackAlgorithm(AlgorithmType.POPULARITY);
                    return strategyRepository.save(s);
                });
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
                .filter(a -> a.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Алгоритм не найден: " + type));
    }
}
