package com.example.recommender.service.algorithm;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.ItemDto;
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
        Map<String, Double> weights = context.getEventWeights();
        int halfLifeDays = Optional.ofNullable(context.getStrategy().getTimeDecayHalfLifeDays()).orElse(30);
        double lambda = Math.log(2) / halfLifeDays;
        Instant now = Instant.now();

        Map<Long, Map<Long, Double>> userItemWeights = new HashMap<>();
        // собираем веса событий пользователя с учетом временного затухания
        for (EventDto event : context.getAllEvents()) {
            double weight = weights.getOrDefault(event.getType(), 1.0);
            long ageDays = Duration.between(event.getCreatedAt(), now).toDays();
            double decay = Math.exp(-lambda * ageDays);
            double score = weight * decay;
            userItemWeights.computeIfAbsent(event.getUserId(), k -> new HashMap<>())
                    .merge(event.getItemId(), score, Double::sum);
        }

        // строим матрицу co-occurrence по всем пользователям
        Map<Long, Map<Long, Double>> similarity = new HashMap<>();
        for (Map<Long, Double> items : userItemWeights.values()) {
            List<Long> ids = items.keySet().stream().toList();
            for (int i = 0; i < ids.size(); i++) {
                Long itemA = ids.get(i);
                for (int j = 0; j < ids.size(); j++) {
                    if (i == j) continue;
                    Long itemB = ids.get(j);
                    double contrib = Math.sqrt(items.get(itemA) * items.get(itemB));
                    similarity.computeIfAbsent(itemA, k -> new HashMap<>())
                            .merge(itemB, contrib, Double::sum);
                }
            }
        }

        if (context.getUserEvents().isEmpty()) {
            return RecommendationResponse.builder()
                    .algorithm(type())
                    .strategyId(context.getStrategy().getId())
                    .generatedAt(now)
                    .items(List.of())
                    .build();
        }

        // считаем кандидатов по похожести на уже просмотренные товары
        Map<Long, Double> candidateScores = new HashMap<>();
        for (Long seen : context.getSeenItemIds()) {
            Map<Long, Double> sims = similarity.getOrDefault(seen, Collections.emptyMap());
            for (Map.Entry<Long, Double> entry : sims.entrySet()) {
                if (context.getSeenItemIds().contains(entry.getKey())) continue;
                candidateScores.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        int limit = Math.min(context.getLimit(),
                Optional.ofNullable(context.getStrategy().getCandidateLimit()).orElse(200));

        List<Long> topIds = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();

        if (topIds.isEmpty()) {
            return RecommendationResponse.builder()
                    .algorithm(type())
                    .strategyId(context.getStrategy().getId())
                    .generatedAt(now)
                    .items(List.of())
                    .build();
        }

        // подтягиваем карточки и отдаем в порядке скоринга
        List<ItemDto> items = catalogClient.getItemsByIds(topIds);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < topIds.size(); i++) {
            order.put(topIds.get(i), i);
        }
        List<RecommendationItemDto> result = items.stream()
                .sorted(Comparator.comparingInt(i -> order.getOrDefault(i.getId(), Integer.MAX_VALUE)))
                .map(i -> RecommendationItemDto.builder()
                        .item(i)
                        .score(candidateScores.getOrDefault(i.getId(), 0.0))
                        .build())
                .collect(Collectors.toList());

        return RecommendationResponse.builder()
                .algorithm(type())
                .strategyId(context.getStrategy().getId())
                .generatedAt(now)
                .items(result)
                .build();
    }
}
