package com.example.recommender.model;

import com.example.recommender.dto.ItemDto;
import com.example.recommender.dto.EventDto;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@Builder
public class RecommendationContext {
    Long userId;
    int limit;
    RecommendationStrategy strategy;
    AlgorithmType requestedAlgorithm;
    List<EventDto> userEvents;
    List<EventDto> allEvents;
    Set<Long> seenItemIds;
    Map<String, Double> eventWeights;
}
