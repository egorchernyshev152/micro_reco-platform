package com.example.recommender.controller;

import com.example.recommender.dto.analytics.ActivityAnalyticsDto;
import com.example.recommender.dto.analytics.AdminAnalyticsSummaryDto;
import com.example.recommender.dto.analytics.PopularityAnalyticsDto;
import com.example.recommender.dto.analytics.RecommendationAnalyticsDto;
import com.example.recommender.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin analytics", description = "Аналитика популярности, активности и рекомендаций")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    @GetMapping("/summary")
    @Operation(summary = "Сводка по ключевым KPI")
    public AdminAnalyticsSummaryDto getSummary(@RequestParam(value = "period", required = false) String period) {
        return analyticsService.getSummary(period);
    }

    @GetMapping("/popularity")
    @Operation(summary = "Популярность фильмов по событиям")
    public PopularityAnalyticsDto getPopularity(@RequestParam(value = "period", required = false) String period,
                                                @RequestParam(value = "limit", required = false) Integer limit) {
        return analyticsService.getPopularity(period, limit);
    }

    @GetMapping("/activity")
    @Operation(summary = "Активность пользователей")
    public ActivityAnalyticsDto getActivity(@RequestParam(value = "period", required = false) String period) {
        return analyticsService.getActivity(period);
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Эффективность рекомендаций")
    public RecommendationAnalyticsDto getRecommendations(@RequestParam(value = "period", required = false) String period) {
        return analyticsService.getRecommendationAnalytics(period);
    }
}
