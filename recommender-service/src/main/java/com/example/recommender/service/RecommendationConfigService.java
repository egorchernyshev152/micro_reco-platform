package com.example.recommender.service;

import com.example.recommender.dto.RecommendationConfigDto;
import com.example.recommender.dto.RecommendationConfigUpdateRequest;
import com.example.recommender.dto.RecommendationRebuildLogDto;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationConfig;
import com.example.recommender.model.RecommendationRebuildLog;
import com.example.recommender.model.RecommendationRebuildStatus;
import com.example.recommender.repository.RecommendationConfigRepository;
import com.example.recommender.repository.RecommendationRebuildLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
@RequiredArgsConstructor
public class RecommendationConfigService {

    private final RecommendationConfigRepository configRepository;
    private final RecommendationRebuildLogRepository logRepository;

    @Transactional(readOnly = true)
    public RecommendationConfigDto getConfig() {
        RecommendationConfig config = configRepository.findById(RecommendationConfig.SINGLETON_ID)
                .orElseGet(this::createDefaults);
        return toDto(config);
    }

    @Transactional
    public RecommendationConfigDto updateConfig(RecommendationConfigUpdateRequest request) {
        RecommendationConfig config = configRepository.findById(RecommendationConfig.SINGLETON_ID)
                .orElseGet(this::createDefaults);
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        if (request.getTrainingPeriod() != null) {
            config.setTrainingPeriod(request.getTrainingPeriod().toUpperCase());
        }
        if (request.getDefaultAlgorithm() != null) {
            config.setDefaultAlgorithm(request.getDefaultAlgorithm());
        }
        if (request.getDefaultStrategyId() != null) {
            config.setDefaultStrategyId(request.getDefaultStrategyId());
        }
        if (request.getRecommendationLimit() != null) {
            config.setRecommendationLimit(request.getRecommendationLimit());
        }
        if (request.getRebuildBatchSize() != null) {
            config.setRebuildBatchSize(request.getRebuildBatchSize());
        }
        if (request.getMaxUsersPerJob() != null) {
            config.setMaxUsersPerJob(request.getMaxUsersPerJob());
        }
        RecommendationConfig saved = configRepository.save(config);
        return toDto(saved);
    }

    @Transactional
    public RecommendationConfig ensureConfig() {
        return configRepository.findById(RecommendationConfig.SINGLETON_ID)
                .orElseGet(this::createDefaults);
    }

    private RecommendationConfigDto toDto(RecommendationConfig config) {
        RecommendationRebuildLogDto active = logRepository
                .findFirstByStatusInOrderByStartedAtDesc(EnumSet.of(
                        RecommendationRebuildStatus.SCHEDULED,
                        RecommendationRebuildStatus.RUNNING))
                .map(this::toLogDto)
                .orElse(null);
        RecommendationRebuildLogDto last = logRepository
                .findFirstByStatusOrderByStartedAtDesc(RecommendationRebuildStatus.COMPLETED)
                .map(this::toLogDto)
                .orElse(null);
        return RecommendationConfigDto.builder()
                .enabled(config.isEnabled())
                .trainingPeriod(config.getTrainingPeriod())
                .defaultAlgorithm(config.getDefaultAlgorithm())
                .defaultStrategyId(config.getDefaultStrategyId())
                .recommendationLimit(config.getRecommendationLimit())
                .rebuildBatchSize(config.getRebuildBatchSize())
                .maxUsersPerJob(config.getMaxUsersPerJob())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .activeRebuild(active)
                .lastRebuild(last)
                .build();
    }

    private RecommendationConfig createDefaults() {
        RecommendationConfig defaults = RecommendationConfig.builder()
                .id(RecommendationConfig.SINGLETON_ID)
                .enabled(true)
                .trainingPeriod("WEEK")
                .defaultAlgorithm(AlgorithmType.HYBRID)
                .recommendationLimit(12)
                .rebuildBatchSize(25)
                .maxUsersPerJob(500)
                .build();
        return configRepository.save(defaults);
    }

    public RecommendationRebuildLogDto toLogDto(RecommendationRebuildLog log) {
        return RecommendationRebuildLogDto.builder()
                .id(log.getId())
                .status(log.getStatus())
                .processedUsers(log.getProcessedUsers())
                .totalUsers(log.getTotalUsers())
                .startedAt(log.getStartedAt())
                .finishedAt(log.getFinishedAt())
                .initiator(log.getInitiator())
                .trainingPeriod(log.getTrainingPeriod())
                .message(log.getMessage())
                .build();
    }
}
