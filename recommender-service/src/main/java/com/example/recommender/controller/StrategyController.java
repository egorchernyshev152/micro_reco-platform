package com.example.recommender.controller;

import com.example.recommender.dto.StrategyCreateRequest;
import com.example.recommender.dto.StrategyResponse;
import com.example.recommender.dto.StrategyUpdateRequest;
import com.example.recommender.service.StrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
@Tag(name = "Strategies", description = "Управление стратегиями рекомендаций")
public class StrategyController {

    private final StrategyService strategyService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать стратегию")
    public StrategyResponse create(@Valid @RequestBody StrategyCreateRequest request) {
        // сохраняем стратегию с параметрами алгоритма
        return strategyService.create(request);
    }

    @GetMapping
    @Operation(summary = "Получить все стратегии")
    public List<StrategyResponse> findAll() {
        // отдаем все сохраненные стратегии
        return strategyService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить стратегию по id")
    public StrategyResponse get(@PathVariable Long id) {
        // находим стратегию по идентификатору
        return strategyService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить стратегию по id")
    public StrategyResponse update(@PathVariable Long id, @Valid @RequestBody StrategyUpdateRequest request) {
        // обновляем выбранную стратегию
        return strategyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить стратегию по id")
    public void delete(@PathVariable Long id) {
        // удаляем стратегию
        strategyService.delete(id);
    }
}
