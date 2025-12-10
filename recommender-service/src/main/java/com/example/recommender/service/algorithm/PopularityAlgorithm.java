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
        double lambda = Math.log(2) / halfLifeDays;
        Instant now = Instant.now();

        // считаем взвешенный балл по популярности с временным затуханием
        for (EventDto event : context.getAllEvents()) {
            double weight = weights.getOrDefault(event.getType(), 1.0);
            long ageDays = Duration.between(event.getCreatedAt(), now).toDays();
            double decay = Math.exp(-lambda * ageDays);
            scores.merge(event.getItemId(), weight * decay, Double::sum);
        }

        List<Long> topIds = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(context.getLimit())
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

        // подтягиваем карточки товаров и сохраняем порядок по убыванию скоринга
        List<ItemDto> items = catalogClient.getItemsByIds(topIds);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < topIds.size(); i++) {
            order.put(topIds.get(i), i);
        }

        List<RecommendationItemDto> result = items.stream()
                .sorted(Comparator.comparingInt(i -> order.getOrDefault(i.getId(), Integer.MAX_VALUE)))
                .map(i -> RecommendationItemDto.builder()
                        .item(i)
                        .score(scores.getOrDefault(i.getId(), 0.0))
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
