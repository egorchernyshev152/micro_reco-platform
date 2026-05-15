package com.example.recommender.controller;

import com.example.recommender.dto.UserPreferenceDto;
import com.example.recommender.dto.UserPreferenceRequest;
import com.example.recommender.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preference Tuning", description = "Настройка вкусов и уточнение рекомендаций")
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Получить предпочтения пользователя для рекомендаций")
    public UserPreferenceDto get(@PathVariable("userId") @Min(1) Long userId) {
        return preferenceService.getPreferences(userId);
    }

    @PutMapping
    @Operation(summary = "Обновить предпочтения пользователя для рекомендаций")
    public UserPreferenceDto update(@PathVariable("userId") @Min(1) Long userId,
                                    @Valid @RequestBody UserPreferenceRequest request) {
        return preferenceService.updatePreferences(userId, request);
    }
}
