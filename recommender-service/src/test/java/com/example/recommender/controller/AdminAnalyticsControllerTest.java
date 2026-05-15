package com.example.recommender.controller;

import com.example.recommender.dto.analytics.ActivityAnalyticsDto;
import com.example.recommender.dto.analytics.AdminAnalyticsSummaryDto;
import com.example.recommender.dto.analytics.PopularMovieDto;
import com.example.recommender.dto.analytics.PopularityAnalyticsDto;
import com.example.recommender.dto.analytics.RecommendationAnalyticsDto;
import com.example.recommender.service.AdminAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAnalyticsController.class)
class AdminAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAnalyticsService analyticsService;

    @Test
    void shouldReturnSummary() throws Exception {
        when(analyticsService.getSummary(eq("WEEK")))
                .thenReturn(AdminAnalyticsSummaryDto.builder()
                        .period("WEEK")
                        .generatedAt(Instant.now())
                        .totalEvents(10)
                        .activeUsers(3)
                        .build());

        mockMvc.perform(get("/api/admin/analytics/summary")
                        .param("period", "WEEK")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("WEEK"))
                .andExpect(jsonPath("$.totalEvents").value(10));

        verify(analyticsService).getSummary("WEEK");
    }

    @Test
    void shouldReturnPopularity() throws Exception {
        when(analyticsService.getPopularity(eq("MONTH"), eq(3)))
                .thenReturn(PopularityAnalyticsDto.builder()
                        .period("MONTH")
                        .generatedAt(Instant.now())
                        .topMovies(List.of(PopularMovieDto.builder()
                                .movieId(1L)
                                .title("Movie")
                                .events(5)
                                .share(0.5)
                                .eventTypes(Map.of("VIEW_CARD", 5L))
                                .build()))
                        .build());

        mockMvc.perform(get("/api/admin/analytics/popularity")
                        .param("period", "MONTH")
                        .param("limit", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("MONTH"))
                .andExpect(jsonPath("$.topMovies[0].title").value("Movie"));

        verify(analyticsService).getPopularity("MONTH", 3);
    }

    @Test
    void shouldReturnRecommendationAnalytics() throws Exception {
        when(analyticsService.getRecommendationAnalytics(eq("DAY")))
                .thenReturn(RecommendationAnalyticsDto.builder()
                        .period("DAY")
                        .generatedAt(Instant.now())
                        .clicks(4)
                        .watchStarts(2)
                        .watchCompletions(1)
                        .build());

        mockMvc.perform(get("/api/admin/analytics/recommendations")
                        .param("period", "DAY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("DAY"))
                .andExpect(jsonPath("$.clicks").value(4));

        verify(analyticsService).getRecommendationAnalytics("DAY");
    }

    @Test
    void shouldReturnActivityAnalytics() throws Exception {
        when(analyticsService.getActivity(eq("WEEK")))
                .thenReturn(ActivityAnalyticsDto.builder()
                        .period("WEEK")
                        .generatedAt(Instant.now())
                        .activeUsers(5)
                        .build());

        mockMvc.perform(get("/api/admin/analytics/activity")
                        .param("period", "WEEK")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeUsers").value(5));

        verify(analyticsService).getActivity("WEEK");
    }
}
