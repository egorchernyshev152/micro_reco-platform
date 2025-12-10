package com.example.recommender.controller;

import com.example.recommender.dto.RecommendationResponse;
import com.example.recommender.model.AlgorithmType;
import com.example.recommender.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Точки получения рекомендаций")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/popular")
    @Operation(summary = "Популярные элементы")
    public RecommendationResponse popular(@RequestParam(value = "period", required = false) String period,
                                          @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit) {
        // отдаем топ по популярности за выбранный период
        return recommendationService.recommendPopular(period, limit);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Персональные рекомендации")
    public RecommendationResponse personalized(@PathVariable("userId") Long userId,
                                               @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit,
                                               @RequestParam(value = "period", required = false) String period,
                                               @RequestParam(value = "algo", required = false) AlgorithmType algo,
                                               @RequestParam(value = "strategyId", required = false) Long strategyId) {
        // персональные рекомендации с выбором алгоритма или стратегии
        return recommendationService.recommendForUser(userId, limit, period, algo, strategyId);
    }

    @PostMapping("/rebuild")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Пересчитать матрицу похожести/кэш")
    public void rebuild() {
        // здесь будет триггер на фоновую задачу пересчета co-occurrence
    }
}
