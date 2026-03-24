package com.example.recommender.controller;

import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
@Tag(name = "Movie Recommendations", description = "Выдача рекомендаций по фильмам")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/popular")
    @Operation(summary = "Популярные фильмы")
    public RecommendationResponse popular(@RequestParam(value = "period", required = false) String period,
                                          @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit) {
        return recommendationService.recommendPopular(period, limit);
    }

    @GetMapping("/trending")
    @Operation(summary = "Трендовые фильмы с ростом событий")
    public RecommendationResponse trending(@RequestParam(value = "period", required = false) String period,
                                           @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit) {
        return recommendationService.recommendTrending(period, limit);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Персональные рекомендации")
    public RecommendationResponse personalized(@PathVariable("userId") Long userId,
                                               @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit,
                                               @RequestParam(value = "period", required = false) String period,
                                               @RequestParam(value = "algo", required = false) AlgorithmType algo,
                                               @RequestParam(value = "strategyId", required = false) Long strategyId) {
        return recommendationService.recommendForUser(userId, limit, period, algo, strategyId);
    }

    @GetMapping("/similar/{movieId}")
    @Operation(summary = "Похожие фильмы по контенту")
    public RecommendationResponse similar(@PathVariable("movieId") Long movieId,
                                          @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit) {
        return recommendationService.recommendSimilar(movieId, limit);
    }
}
