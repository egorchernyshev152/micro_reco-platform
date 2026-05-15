package com.example.recommender.service;

import com.example.recommender.dto.RecommendationRebuildLogDto;
import com.example.recommender.dto.RecommendationRebuildRequest;
import com.example.recommender.dto.UserStatDto;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationConfig;
import com.example.recommender.model.RecommendationRebuildLog;
import com.example.recommender.model.RecommendationRebuildStatus;
import com.example.recommender.repository.RecommendationConfigRepository;
import com.example.recommender.repository.RecommendationRebuildLogRepository;
import com.example.recommender.client.EventClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationRebuildService {

    private final RecommendationRebuildLogRepository logRepository;
    private final RecommendationConfigRepository configRepository;
    private final RecommendationConfigService configService;
    private final RecommendationService recommendationService;
    private final EventClient eventClient;
    private final ApplicationEventPublisher events;
    @Qualifier("rebuildTaskExecutor")
    private final TaskExecutor rebuildTaskExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Transactional
    public RecommendationRebuildLogDto triggerRebuild(RecommendationRebuildRequest request) {
        RecommendationConfig config = configRepository.findById(RecommendationConfig.SINGLETON_ID)
                .orElseGet(configService::ensureConfig);
        if (!config.isEnabled()) {
            throw new IllegalStateException("Рекомендации отключены, пересчет невозможен");
        }
        if (running.get()) {
            throw new IllegalStateException("Пересчет уже выполняется");
        }
        logRepository.findFirstByStatusInOrderByStartedAtDesc(EnumSet.of(
                RecommendationRebuildStatus.SCHEDULED,
                RecommendationRebuildStatus.RUNNING)).ifPresent(log -> {
            throw new IllegalStateException("Пересчет уже выполняется");
        });

        RecommendationRebuildLog logEntry = RecommendationRebuildLog.builder()
                .status(RecommendationRebuildStatus.SCHEDULED)
                .initiator(request != null ? request.getInitiator() : null)
                .trainingPeriod(config.getTrainingPeriod())
                .totalUsers(0)
                .processedUsers(0)
                .startedAt(Instant.now())
                .build();
        RecommendationRebuildLog saved = logRepository.saveAndFlush(logEntry);
        scheduleRebuild(saved.getId(), config);
        return configService.toLogDto(saved);
    }

    private void scheduleRebuild(Long logId, RecommendationConfig config) {
        Runnable task = () -> runRebuild(logId, config);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    startRebuild(task);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        running.set(false);
                    }
                }
            });
        } else {
            startRebuild(task);
        }
    }

    private void startRebuild(Runnable task) {
        running.set(true);
        rebuildTaskExecutor.execute(task);
    }

    private void runRebuild(Long logId, RecommendationConfig config) {
        RecommendationRebuildLog logEntry = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalStateException("Запуск пересчета не найден: " + logId));
        logEntry.setStatus(RecommendationRebuildStatus.RUNNING);
        logRepository.save(logEntry);
        publishProgress(logEntry);
        try {
            List<UserStatDto> stats = eventClient.getUserStats(config.getTrainingPeriod());
            if (CollectionUtils.isEmpty(stats)) {
                stats = List.of();
            }
            int totalUsers = Math.min(stats.size(), config.getMaxUsersPerJob());
            logEntry.setTotalUsers(totalUsers);
            logRepository.save(logEntry);
            int processed = 0;
            for (UserStatDto stat : stats.stream().limit(totalUsers).toList()) {
                try {
                    AlgorithmType algorithm = config.getDefaultAlgorithm();
                    recommendationService.recommendForUser(
                            stat.getUserId(),
                            config.getRecommendationLimit(),
                            config.getTrainingPeriod(),
                            algorithm,
                            config.getDefaultStrategyId());
                } catch (Exception ex) {
                    log.warn("Не удалось построить рекомендации для пользователя {}: {}", stat.getUserId(), ex.getMessage());
                }
                processed++;
                if (processed % Math.max(1, config.getRebuildBatchSize()) == 0 || processed == totalUsers) {
                    logEntry.setProcessedUsers(processed);
                    logRepository.save(logEntry);
                    publishProgress(logEntry);
                }
            }
            logEntry.setProcessedUsers(totalUsers);
            logEntry.setStatus(RecommendationRebuildStatus.COMPLETED);
            logEntry.setFinishedAt(Instant.now());
            logRepository.save(logEntry);
            publishProgress(logEntry);
        } catch (Exception ex) {
            logEntry.setStatus(RecommendationRebuildStatus.FAILED);
            logEntry.setFinishedAt(Instant.now());
            logEntry.setMessage(ex.getMessage());
            logRepository.save(logEntry);
            publishProgress(logEntry);
            log.error("Ошибка пересчета рекомендаций: {}", ex.getMessage(), ex);
        } finally {
            running.set(false);
        }
    }

    private void publishProgress(RecommendationRebuildLog logEntry) {
        events.publishEvent(new RebuildProgressEvent(
                logEntry.getId(),
                logEntry.getProcessedUsers() == null ? 0 : logEntry.getProcessedUsers(),
                logEntry.getTotalUsers() == null ? 0 : logEntry.getTotalUsers(),
                logEntry.getStatus()
        ));
    }
}
