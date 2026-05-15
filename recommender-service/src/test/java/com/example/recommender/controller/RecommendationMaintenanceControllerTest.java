package com.example.recommender.controller;

import com.example.recommender.client.EventClient;
import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.dto.UserStatDto;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.model.RecommendationRebuildStatus;
import com.example.recommender.repository.RecommendationRebuildLogRepository;
import com.example.recommender.service.RecommendationConfigService;
import com.example.recommender.service.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationMaintenanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecommendationConfigService configService;

    @Autowired
    private RecommendationRebuildLogRepository logRepository;

    @MockBean
    private EventClient eventClient;

    @MockBean
    private RecommendationService recommendationService;

    @BeforeEach
    void setup() {
        logRepository.deleteAll();
    }

    @Test
    void shouldGetAndUpdateConfig() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingPeriod").value("WEEK"));

        String payload = """
                {
                  "trainingPeriod": "DAY",
                  "defaultAlgorithm": "POPULARITY",
                  "recommendationLimit": 5,
                  "rebuildBatchSize": 2,
                  "maxUsersPerJob": 10,
                  "enabled": false
                }
                """;

        mockMvc.perform(put("/api/v1/recommendations/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trainingPeriod").value("DAY"))
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void shouldStartRebuildJob() throws Exception {
        when(eventClient.getUserStats(anyString()))
                .thenReturn(List.of(
                        new UserStatDto(1L, 10L),
                        new UserStatDto(2L, 5L)));
        when(recommendationService.recommendForUser(anyLong(), anyInt(), anyString(), any(), any()))
                .thenReturn(RecommendationResponse.builder()
                        .algorithm(AlgorithmType.POPULARITY)
                        .build());

        mockMvc.perform(post("/api/v1/recommendations/rebuild")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"initiator\":\"test\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        var logEntry = logRepository.findAll().get(0);
        assertThat(logEntry.getStatus()).isEqualTo(RecommendationRebuildStatus.COMPLETED);
        assertThat(logEntry.getProcessedUsers()).isEqualTo(logEntry.getTotalUsers());
    }

    @TestConfiguration
    static class SyncExecutorConfiguration {
        @Bean
        @Primary
        public TaskExecutor rebuildTaskExecutor() {
            return new SyncTaskExecutor();
        }
    }
}
