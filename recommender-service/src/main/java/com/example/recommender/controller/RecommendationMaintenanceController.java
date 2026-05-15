package com.example.recommender.controller;

import com.example.recommender.dto.RecommendationConfigDto;
import com.example.recommender.dto.RecommendationConfigUpdateRequest;
import com.example.recommender.dto.RecommendationRebuildLogDto;
import com.example.recommender.dto.RecommendationRebuildRequest;
import com.example.recommender.service.RecommendationConfigService;
import com.example.recommender.service.RecommendationRebuildService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendation config", description = "Управление настройками и пересчетом рекомендаций")
public class RecommendationMaintenanceController {

    private final RecommendationConfigService configService;
    private final RecommendationRebuildService rebuildService;

    @GetMapping("/config")
    @Operation(summary = "Получить конфигурацию рекомендаций")
    public RecommendationConfigDto getConfig() {
        return configService.getConfig();
    }

    @PutMapping("/config")
    @Operation(summary = "Обновить конфигурацию рекомендаций")
    public RecommendationConfigDto updateConfig(@Valid @RequestBody RecommendationConfigUpdateRequest request) {
        return configService.updateConfig(request);
    }

    @PostMapping("/rebuild")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Запустить пересчет рекомендаций")
    public RecommendationRebuildLogDto rebuild(@Valid @RequestBody(required = false) RecommendationRebuildRequest request) {
        return rebuildService.triggerRebuild(request == null ? new RecommendationRebuildRequest() : request);
    }
}
