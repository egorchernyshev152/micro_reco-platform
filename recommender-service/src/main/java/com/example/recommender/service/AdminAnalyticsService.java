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
import com.example.recommender.dto.analytics.DailyMetricPointDto;
import com.example.recommender.dto.analytics.HourlyMetricPointDto;
import com.example.recommender.dto.analytics.PopularMovieDto;
import com.example.recommender.dto.analytics.PopularityAnalyticsDto;
import com.example.recommender.dto.analytics.PopularityTrendPointDto;
import com.example.recommender.dto.analytics.RecommendationAlgorithmBreakdownDto;
import com.example.recommender.dto.analytics.RecommendationAnalyticsDto;
import com.example.recommender.dto.analytics.RecommendationTrendPointDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsService {

    private static final String DEFAULT_PERIOD = "WEEK";
    private static final String RECOMMENDER_SOURCE = "RECOMMENDER";
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withLocale(Locale.US)
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:00")
            .withLocale(Locale.US)
            .withZone(ZoneOffset.UTC);
    private static final int DEFAULT_TOP_MOVIES = 5;
    private static final int MOVIE_EVENT_LIMIT = 400;
    private static final int RECOMMENDATION_EVENT_LIMIT = 3000;
    private static final int TREND_MOVIE_LIMIT = 3;

    private final EventClient eventClient;
    private final CatalogClient catalogClient;

    public AdminAnalyticsSummaryDto getSummary(String period) {
        String normalizedPeriod = normalizePeriod(period);
        List<DayStatDto> dayStats = safeList(eventClient.getDailyStats(normalizedPeriod));
        List<UserStatDto> userStats = safeList(eventClient.getUserStats(normalizedPeriod));
        List<EventDto> recommendationEvents = fetchRecommendationEvents(normalizedPeriod);

        long totalEvents = dayStats.stream().mapToLong(stat -> safeLong(stat.getCount())).sum();
        long activeUsers = userStats.size();
        double avgEventsPerUser = activeUsers == 0 ? 0 : round((double) totalEvents / activeUsers);
        long recommendationClicks = recommendationEvents.stream()
                .filter(event -> "VIEW_CARD".equalsIgnoreCase(event.getType()))
                .count();
        long recommendationStarts = recommendationEvents.stream()
                .filter(event -> "START_WATCHING".equalsIgnoreCase(event.getType()))
                .count();
        double conversion = recommendationClicks == 0 ? 0 : round((double) recommendationStarts / recommendationClicks);

        List<DailyMetricPointDto> trend = dayStats.stream()
                .sorted(Comparator.comparing(DayStatDto::getDay))
                .map(stat -> DailyMetricPointDto.builder()
                        .day(stat.getDay())
                        .events(safeLong(stat.getCount()))
                        .activeUsers(null)
                        .build())
                .toList();

        return AdminAnalyticsSummaryDto.builder()
                .period(normalizedPeriod)
                .generatedAt(Instant.now())
                .totalEvents(totalEvents)
                .activeUsers(activeUsers)
                .avgEventsPerUser(avgEventsPerUser)
                .recommendationClicks(recommendationClicks)
                .recommendationStarts(recommendationStarts)
                .recommendationConversion(conversion)
                .trend(trend)
                .build();
    }

    public PopularityAnalyticsDto getPopularity(String period, Integer limit) {
        String normalizedPeriod = normalizePeriod(period);
        int topLimit = limit == null || limit <= 0 ? DEFAULT_TOP_MOVIES : Math.min(limit, 10);
        List<MovieStatDto> stats = safeList(eventClient.getStatsByMovie(normalizedPeriod));
        long totalEvents = stats.stream().mapToLong(stat -> safeLong(stat.getCount())).sum();

        List<MovieStatDto> topStats = stats.stream()
                .sorted(Comparator.comparing(MovieStatDto::getCount).reversed())
                .limit(topLimit)
                .toList();
        Set<Long> topMovieIds = topStats.stream()
                .map(MovieStatDto::getMovieId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, MovieDto> movies = catalogClient.getMoviesByIds(topMovieIds).stream()
                .collect(Collectors.toMap(MovieDto::getId, Function.identity()));

        List<PopularMovieDto> topMovies = topStats.stream()
                .map(stat -> {
                    MovieDto movie = movies.get(stat.getMovieId());
                    List<EventDto> events = safeList(eventClient.getEvents(null, stat.getMovieId(), null, normalizedPeriod,
                            MOVIE_EVENT_LIMIT, null));
                    Map<String, Long> typeBreakdown = events.stream()
                            .collect(Collectors.groupingBy(event -> event.getType() == null ? "UNKNOWN" : event.getType(),
                                    Collectors.counting()));
                    double share = totalEvents == 0 ? 0 : round((double) safeLong(stat.getCount()) / totalEvents);
                    return PopularMovieDto.builder()
                            .movieId(stat.getMovieId())
                            .title(movie != null ? movie.getTitle() : ("Movie #" + stat.getMovieId()))
                            .posterUrl(movie != null ? movie.getPosterUrl() : null)
                            .events(safeLong(stat.getCount()))
                            .share(share)
                            .eventTypes(typeBreakdown)
                            .build();
                })
                .toList();

        List<Long> trendMovieIds = topMovies.stream()
                .map(PopularMovieDto::getMovieId)
                .limit(TREND_MOVIE_LIMIT)
                .toList();
        List<PopularityTrendPointDto> trend = buildPopularityTrend(normalizedPeriod, trendMovieIds);

        return PopularityAnalyticsDto.builder()
                .period(normalizedPeriod)
                .generatedAt(Instant.now())
                .topMovies(topMovies)
                .trend(trend)
                .build();
    }

    public ActivityAnalyticsDto getActivity(String period) {
        String normalizedPeriod = normalizePeriod(period);
        List<UserStatDto> userStats = safeList(eventClient.getUserStats(normalizedPeriod));
        List<DayStatDto> dayStats = safeList(eventClient.getDailyStats(normalizedPeriod));
        List<TimeDistributionStatDto> hourly = safeList(eventClient.getTimeDistribution(normalizedPeriod));

        long activeUsers = userStats.size();
        long events = dayStats.stream().mapToLong(stat -> safeLong(stat.getCount())).sum();
        double avgEventsPerUser = activeUsers == 0 ? 0 : round((double) events / activeUsers);

        List<ActivitySegmentDto> segments = buildSegments(userStats);
        List<DailyMetricPointDto> trend = dayStats.stream()
                .sorted(Comparator.comparing(DayStatDto::getDay))
                .map(stat -> DailyMetricPointDto.builder()
                        .day(stat.getDay())
                        .events(safeLong(stat.getCount()))
                        .activeUsers(null)
                        .build())
                .toList();
        List<HourlyMetricPointDto> hourlyDistribution = aggregateHourly(hourly);

        return ActivityAnalyticsDto.builder()
                .period(normalizedPeriod)
                .generatedAt(Instant.now())
                .activeUsers(activeUsers)
                .avgEventsPerUser(avgEventsPerUser)
                .segments(segments)
                .trend(trend)
                .hourlyDistribution(hourlyDistribution)
                .build();
    }

    public RecommendationAnalyticsDto getRecommendationAnalytics(String period) {
        String normalizedPeriod = normalizePeriod(period);
        List<EventDto> recommendationEvents = fetchRecommendationEvents(normalizedPeriod);

        long clicks = recommendationEvents.stream()
                .filter(event -> "VIEW_CARD".equalsIgnoreCase(event.getType()))
                .count();
        long starts = recommendationEvents.stream()
                .filter(event -> "START_WATCHING".equalsIgnoreCase(event.getType()))
                .count();
        long finishes = recommendationEvents.stream()
                .filter(event -> "FINISH_WATCHING".equalsIgnoreCase(event.getType()))
                .count();
        long ratings = recommendationEvents.stream()
                .filter(event -> "RATE".equalsIgnoreCase(event.getType()))
                .count();

        double conversion = clicks == 0 ? 0 : round((double) starts / clicks);
        double completion = starts == 0 ? 0 : round((double) finishes / starts);

        Map<String, List<EventDto>> byAlgorithm = recommendationEvents.stream()
                .collect(Collectors.groupingBy(this::resolveAlgorithm));

        List<RecommendationAlgorithmBreakdownDto> algorithms = byAlgorithm.entrySet().stream()
                .map(entry -> RecommendationAlgorithmBreakdownDto.builder()
                        .algorithm(entry.getKey())
                        .views(entry.getValue().stream().filter(evt -> "VIEW_CARD".equalsIgnoreCase(evt.getType())).count())
                        .starts(entry.getValue().stream().filter(evt -> "START_WATCHING".equalsIgnoreCase(evt.getType())).count())
                        .finishes(entry.getValue().stream().filter(evt -> "FINISH_WATCHING".equalsIgnoreCase(evt.getType())).count())
                        .ratings(entry.getValue().stream().filter(evt -> "RATE".equalsIgnoreCase(evt.getType())).count())
                        .build())
                .sorted(Comparator.comparingLong(RecommendationAlgorithmBreakdownDto::getViews).reversed())
                .toList();

        List<RecommendationTrendPointDto> trend = buildRecommendationTrend(recommendationEvents);

        return RecommendationAnalyticsDto.builder()
                .period(normalizedPeriod)
                .generatedAt(Instant.now())
                .clicks(clicks)
                .watchStarts(starts)
                .watchCompletions(finishes)
                .ratings(ratings)
                .conversionRate(conversion)
                .completionRate(completion)
                .algorithms(algorithms)
                .trend(trend)
                .build();
    }

    private List<EventDto> fetchRecommendationEvents(String period) {
        return safeList(eventClient.getEvents(null, null, null, period, RECOMMENDATION_EVENT_LIMIT, RECOMMENDER_SOURCE));
    }

    private List<PopularityTrendPointDto> buildPopularityTrend(String period, List<Long> movieIds) {
        List<PopularityTrendPointDto> points = new ArrayList<>();
        for (Long movieId : movieIds) {
            List<EventDto> events = safeList(eventClient.getEvents(null, movieId, null, period, MOVIE_EVENT_LIMIT, null));
            Map<String, Long> byDay = events.stream()
                    .filter(event -> event.getCreatedAt() != null)
                    .collect(Collectors.groupingBy(event -> DAY_FORMATTER.format(event.getCreatedAt()), Collectors.counting()));
            byDay.forEach((day, count) -> points.add(PopularityTrendPointDto.builder()
                    .day(day)
                    .movieId(movieId)
                    .events(count)
                    .build()));
        }
        points.sort(Comparator.comparing(PopularityTrendPointDto::getDay).thenComparing(PopularityTrendPointDto::getMovieId));
        return points;
    }

    private List<ActivitySegmentDto> buildSegments(List<UserStatDto> stats) {
        List<ActivitySegmentDto> segments = new ArrayList<>();
        segments.add(buildSegment("POWER_USERS", stats, 20, null));
        segments.add(buildSegment("ENGAGED", stats, 5, 19));
        segments.add(buildSegment("CASUAL", stats, 0, 4));
        return segments;
    }

    private ActivitySegmentDto buildSegment(String label, List<UserStatDto> stats, int min, Integer max) {
        List<UserStatDto> filtered = stats.stream()
                .filter(stat -> {
                    long count = safeLong(stat.getCount());
                    if (count < min) return false;
                    return max == null || count <= max;
                })
                .toList();
        DoubleSummaryStatistics aggregate = filtered.stream()
                .mapToDouble(stat -> safeLong(stat.getCount()))
                .summaryStatistics();
        double avg = aggregate.getCount() == 0 ? 0 : round(aggregate.getAverage());
        return ActivitySegmentDto.builder()
                .segment(label)
                .users(aggregate.getCount())
                .avgEvents(avg)
                .build();
    }

    private List<HourlyMetricPointDto> aggregateHourly(List<TimeDistributionStatDto> hourly) {
        Map<String, Long> byHour = hourly.stream()
                .filter(item -> item.getBucket() != null)
                .collect(Collectors.groupingBy(item -> extractHour(item.getBucket()),
                        LinkedHashMap::new,
                        Collectors.summingLong(item -> safeLong(item.getCount()))));
        return byHour.entrySet().stream()
                .map(entry -> HourlyMetricPointDto.builder()
                        .hour(entry.getKey())
                        .events(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(HourlyMetricPointDto::getHour))
                .toList();
    }

    private String extractHour(String bucket) {
        try {
            return bucket.substring(11, 16);
        } catch (Exception ex) {
            return bucket;
        }
    }

    private List<RecommendationTrendPointDto> buildRecommendationTrend(List<EventDto> events) {
        Map<String, List<EventDto>> grouped = events.stream()
                .filter(event -> event.getCreatedAt() != null)
                .collect(Collectors.groupingBy(event -> DAY_FORMATTER.format(event.getCreatedAt())));

        return grouped.entrySet().stream()
                .map(entry -> RecommendationTrendPointDto.builder()
                        .day(entry.getKey())
                        .views(entry.getValue().stream().filter(evt -> "VIEW_CARD".equalsIgnoreCase(evt.getType())).count())
                        .starts(entry.getValue().stream().filter(evt -> "START_WATCHING".equalsIgnoreCase(evt.getType())).count())
                        .finishes(entry.getValue().stream().filter(evt -> "FINISH_WATCHING".equalsIgnoreCase(evt.getType())).count())
                        .build())
                .sorted(Comparator.comparing(RecommendationTrendPointDto::getDay))
                .toList();
    }

    private String resolveAlgorithm(EventDto event) {
        if (event.getPayload() == null) {
            return "UNSPECIFIED";
        }
        Object algo = event.getPayload().get("algorithm");
        if (algo == null) {
            return "UNSPECIFIED";
        }
        return algo.toString().toUpperCase(Locale.ROOT);
    }

    private long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return DEFAULT_PERIOD;
        }
        return switch (period.trim().toUpperCase(Locale.ROOT)) {
            case "DAY", "WEEK", "MONTH" -> period.trim().toUpperCase(Locale.ROOT);
            default -> DEFAULT_PERIOD;
        };
    }
}
