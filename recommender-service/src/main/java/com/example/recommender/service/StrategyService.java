package com.example.recommender.service;

import com.example.recommender.dto.StrategyCreateRequest;
import com.example.recommender.dto.StrategyResponse;
import com.example.recommender.dto.StrategyUpdateRequest;
import com.example.recommender.model.RecommendationStrategy;
import com.example.recommender.repository.RecommendationStrategyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyService {

    private final RecommendationStrategyRepository repository;

    @Transactional
    public StrategyResponse create(StrategyCreateRequest request) {
        // создаем стратегию с заданными параметрами алгоритма и весами
        RecommendationStrategy strategy = new RecommendationStrategy();
        strategy.setName(request.getName());
        strategy.setAlgorithm(request.getAlgorithm());
        strategy.setEventWeights(request.getEventWeights());
        strategy.setTimeDecayHalfLifeDays(request.getTimeDecayHalfLifeDays());
        strategy.setMinEventsPerUser(request.getMinEventsPerUser());
        strategy.setCandidateLimit(request.getCandidateLimit());
        strategy.setFallbackAlgorithm(request.getFallbackAlgorithm());
        strategy.setActive(request.getIsActive() == null || request.getIsActive());
        RecommendationStrategy saved = repository.save(strategy);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StrategyResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StrategyResponse findById(Long id) {
        return repository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Стратегия не найдена: " + id));
    }

    @Transactional
    public StrategyResponse update(Long id, StrategyUpdateRequest request) {
        // обновляем стратегию и оставляем флаг активности если не передан
        RecommendationStrategy strategy = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Стратегия не найдена: " + id));
        strategy.setName(request.getName());
        strategy.setAlgorithm(request.getAlgorithm());
        strategy.setEventWeights(request.getEventWeights());
        strategy.setTimeDecayHalfLifeDays(request.getTimeDecayHalfLifeDays());
        strategy.setMinEventsPerUser(request.getMinEventsPerUser());
        strategy.setCandidateLimit(request.getCandidateLimit());
        strategy.setFallbackAlgorithm(request.getFallbackAlgorithm());
        if (request.getIsActive() != null) {
            strategy.setActive(request.getIsActive());
        }
        return toResponse(repository.save(strategy));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Стратегия не найдена: " + id);
        }
        repository.deleteById(id);
    }

    private StrategyResponse toResponse(RecommendationStrategy s) {
        return StrategyResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .algorithm(s.getAlgorithm())
                .eventWeights(s.getEventWeights())
                .timeDecayHalfLifeDays(s.getTimeDecayHalfLifeDays())
                .minEventsPerUser(s.getMinEventsPerUser())
                .candidateLimit(s.getCandidateLimit())
                .fallbackAlgorithm(s.getFallbackAlgorithm())
                .isActive(s.isActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
