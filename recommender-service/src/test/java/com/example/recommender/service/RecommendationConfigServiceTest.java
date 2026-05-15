package com.example.recommender.service;

import com.example.recommender.dto.RecommendationConfigDto;
import com.example.recommender.dto.RecommendationConfigUpdateRequest;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationConfig;
import com.example.recommender.repository.RecommendationConfigRepository;
import com.example.recommender.repository.RecommendationRebuildLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationConfigServiceTest {

    @Mock
    private RecommendationConfigRepository configRepository;

    @Mock
    private RecommendationRebuildLogRepository logRepository;

    private RecommendationConfigService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationConfigService(configRepository, logRepository);
    }

    @Test
    void shouldCreateDefaultsWhenConfigMissing() {
        when(configRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(configRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(logRepository.findFirstByStatusInOrderByStartedAtDesc(any())).thenReturn(Optional.empty());
        when(logRepository.findFirstByStatusOrderByStartedAtDesc(any())).thenReturn(Optional.empty());

        RecommendationConfigDto dto = service.getConfig();

        assertThat(dto.isEnabled()).isTrue();
        assertThat(dto.getTrainingPeriod()).isEqualTo("WEEK");
        assertThat(dto.getDefaultAlgorithm()).isEqualTo(AlgorithmType.HYBRID);
        assertThat(dto.getRecommendationLimit()).isEqualTo(12);
        assertThat(dto.getRebuildBatchSize()).isEqualTo(25);
    }

    @Test
    void shouldUpdateConfigFields() {
        RecommendationConfig existing = RecommendationConfig.builder()
                .id(RecommendationConfig.SINGLETON_ID)
                .enabled(true)
                .trainingPeriod("WEEK")
                .defaultAlgorithm(AlgorithmType.HYBRID)
                .recommendationLimit(12)
                .rebuildBatchSize(25)
                .maxUsersPerJob(500)
                .build();
        when(configRepository.findById(anyLong())).thenReturn(Optional.of(existing));
        when(configRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(logRepository.findFirstByStatusInOrderByStartedAtDesc(any())).thenReturn(Optional.empty());
        when(logRepository.findFirstByStatusOrderByStartedAtDesc(any())).thenReturn(Optional.empty());

        RecommendationConfigUpdateRequest request = new RecommendationConfigUpdateRequest();
        request.setEnabled(false);
        request.setTrainingPeriod("MONTH");
        request.setDefaultAlgorithm(AlgorithmType.CONTENT_BASED);
        request.setRecommendationLimit(20);
        request.setRebuildBatchSize(10);
        request.setMaxUsersPerJob(50);

        RecommendationConfigDto dto = service.updateConfig(request);

        assertThat(dto.isEnabled()).isFalse();
        assertThat(dto.getTrainingPeriod()).isEqualTo("MONTH");
        assertThat(dto.getDefaultAlgorithm()).isEqualTo(AlgorithmType.CONTENT_BASED);
        assertThat(dto.getRecommendationLimit()).isEqualTo(20);
        assertThat(dto.getRebuildBatchSize()).isEqualTo(10);
        assertThat(dto.getMaxUsersPerJob()).isEqualTo(50);
    }
}
