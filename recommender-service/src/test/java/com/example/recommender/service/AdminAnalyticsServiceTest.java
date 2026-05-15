package com.example.recommender.service;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.client.EventClient;
import com.example.recommender.dto.DayStatDto;
import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.MovieDto;
import com.example.recommender.dto.MovieStatDto;
import com.example.recommender.dto.TimeDistributionStatDto;
import com.example.recommender.dto.UserStatDto;
import com.example.recommender.dto.analytics.ActivityAnalyticsDto;
import com.example.recommender.dto.analytics.ActivitySegmentDto;
import com.example.recommender.dto.analytics.AdminAnalyticsSummaryDto;
import com.example.recommender.dto.analytics.PopularityAnalyticsDto;
import com.example.recommender.dto.analytics.RecommendationAnalyticsDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceTest {

    @Mock
    private EventClient eventClient;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private AdminAnalyticsService service;

    @Test
    void shouldBuildSummaryFromStats() {
        when(eventClient.getDailyStats("WEEK")).thenReturn(List.of(
                new DayStatDto("2024-01-01", 10L),
                new DayStatDto("2024-01-02", 5L)
        ));
        when(eventClient.getUserStats("WEEK")).thenReturn(List.of(
                new UserStatDto(1L, 7L),
                new UserStatDto(2L, 8L)
        ));
        when(eventClient.getEvents(isNull(), isNull(), isNull(), eq("WEEK"), anyInt(), eq("RECOMMENDER")))
                .thenReturn(List.of(
                        EventDto.builder().type("VIEW_CARD").build(),
                        EventDto.builder().type("VIEW_CARD").build(),
                        EventDto.builder().type("START_WATCHING").build()
                ));

        AdminAnalyticsSummaryDto summary = service.getSummary("week");

        assertThat(summary.getTotalEvents()).isEqualTo(15);
        assertThat(summary.getActiveUsers()).isEqualTo(2);
        assertThat(summary.getAvgEventsPerUser()).isEqualTo(7.5);
        assertThat(summary.getRecommendationClicks()).isEqualTo(2);
        assertThat(summary.getRecommendationStarts()).isEqualTo(1);
        assertThat(summary.getRecommendationConversion()).isEqualTo(0.5);
        assertThat(summary.getTrend()).hasSize(2);
    }

    @Test
    void shouldReturnTopMoviesWithTrend() {
        when(eventClient.getStatsByMovie("WEEK")).thenReturn(List.of(
                new MovieStatDto(1L, 12L),
                new MovieStatDto(2L, 4L)
        ));
        when(catalogClient.getMoviesByIds(anyCollection())).thenReturn(List.of(
                MovieDto.builder().id(1L).title("Movie A").posterUrl("poster-a").build(),
                MovieDto.builder().id(2L).title("Movie B").posterUrl("poster-b").build()
        ));
        when(eventClient.getEvents(isNull(), eq(1L), isNull(), eq("WEEK"), anyInt(), isNull())).thenReturn(List.of(
                EventDto.builder().movieId(1L).type("VIEW_CARD").createdAt(Instant.parse("2024-01-01T10:00:00Z")).build(),
                EventDto.builder().movieId(1L).type("RATE").createdAt(Instant.parse("2024-01-02T11:00:00Z")).build()
        ));
        when(eventClient.getEvents(isNull(), eq(2L), isNull(), eq("WEEK"), anyInt(), isNull())).thenReturn(List.of(
                EventDto.builder().movieId(2L).type("VIEW_CARD").createdAt(Instant.parse("2024-01-01T09:00:00Z")).build()
        ));

        PopularityAnalyticsDto dto = service.getPopularity("WEEK", 2);

        assertThat(dto.getTopMovies()).hasSize(2);
        assertThat(dto.getTopMovies().get(0).getTitle()).isEqualTo("Movie A");
        assertThat(dto.getTopMovies().get(0).getEventTypes().get("VIEW_CARD")).isEqualTo(1L);
        assertThat(dto.getTrend()).isNotEmpty();
    }

    @Test
    void shouldCalculateActivitySegments() {
        when(eventClient.getUserStats("WEEK")).thenReturn(List.of(
                new UserStatDto(1L, 25L),
                new UserStatDto(2L, 10L),
                new UserStatDto(3L, 2L)
        ));
        when(eventClient.getDailyStats("WEEK")).thenReturn(List.of(new DayStatDto("2024-01-01", 37L)));
        when(eventClient.getTimeDistribution("WEEK")).thenReturn(List.of(
                new TimeDistributionStatDto("2024-01-01 10:00", 5L),
                new TimeDistributionStatDto("2024-01-01 11:00", 3L)
        ));

        ActivityAnalyticsDto dto = service.getActivity("WEEK");

        assertThat(dto.getActiveUsers()).isEqualTo(3);
        assertThat(dto.getSegments()).extracting(ActivitySegmentDto::getSegment)
                .containsExactly("POWER_USERS", "ENGAGED", "CASUAL");
        assertThat(dto.getHourlyDistribution()).hasSize(2);
    }

    @Test
    void shouldAggregateRecommendationAnalytics() {
        when(eventClient.getEvents(isNull(), isNull(), isNull(), eq("WEEK"), anyInt(), eq("RECOMMENDER")))
                .thenReturn(List.of(
                        EventDto.builder()
                                .type("VIEW_CARD")
                                .createdAt(Instant.parse("2024-01-01T10:00:00Z"))
                                .payload(Map.of("algorithm", "hybrid"))
                                .build(),
                        EventDto.builder()
                                .type("START_WATCHING")
                                .createdAt(Instant.parse("2024-01-01T11:00:00Z"))
                                .payload(Map.of("algorithm", "hybrid"))
                                .build(),
                        EventDto.builder()
                                .type("FINISH_WATCHING")
                                .createdAt(Instant.parse("2024-01-02T11:00:00Z"))
                                .payload(Map.of("algorithm", "cf"))
                                .build()
                ));

        RecommendationAnalyticsDto dto = service.getRecommendationAnalytics("WEEK");

        assertThat(dto.getClicks()).isEqualTo(1);
        assertThat(dto.getWatchStarts()).isEqualTo(1);
        assertThat(dto.getWatchCompletions()).isEqualTo(1);
        assertThat(dto.getAlgorithms()).hasSize(2);
        assertThat(dto.getTrend()).hasSize(2);
    }
}
