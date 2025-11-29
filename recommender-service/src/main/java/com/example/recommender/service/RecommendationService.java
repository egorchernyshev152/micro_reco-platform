package com.example.recommender.service;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.client.EventClient;
import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.ItemDto;
import com.example.recommender.dto.ItemStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final EventClient eventClient;
    private final CatalogClient catalogClient;

    public List<ItemDto> popularRecommendations(String period, int limit) {
        List<ItemStatDto> stats = eventClient.getStatsByItem(period);
        List<Long> topIds = stats.stream()
                .sorted(Comparator.comparing(ItemStatDto::getCount).reversed())
                .limit(limit)
                .map(ItemStatDto::getItemId)
                .toList();
        if (topIds.isEmpty()) return List.of();
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < topIds.size(); i++) order.put(topIds.get(i), i);
        List<ItemDto> items = catalogClient.getItemsByIds(topIds);
        return items.stream()
                .sorted(Comparator.comparingInt(i -> order.getOrDefault(i.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    public List<ItemDto> personalizedRecommendations(Long userId, int limit) {
        List<EventDto> events = eventClient.getEventsByUser(userId);
        if (events.isEmpty()) {
            return popularRecommendations("MONTH", limit);
        }
        Set<Long> seenItemIds = events.stream().map(EventDto::getItemId).collect(Collectors.toSet());
        List<ItemDto> seenItems = catalogClient.getItemsByIds(seenItemIds);
        Map<String, Long> categoryFreq = seenItems.stream()
                .collect(Collectors.groupingBy(ItemDto::getCategory, Collectors.counting()));
        List<String> topCategories = categoryFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey).limit(3).toList();
        List<ItemDto> candidates = catalogClient.getItemsByCategories(topCategories);
        List<ItemDto> filtered = candidates.stream()
                .filter(i -> !seenItemIds.contains(i.getId()))
                .collect(Collectors.toList());
        return filtered.stream().limit(limit).toList();
    }
}

