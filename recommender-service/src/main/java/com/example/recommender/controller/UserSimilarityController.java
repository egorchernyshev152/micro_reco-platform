package com.example.recommender.controller;

import com.example.recommender.dto.SimilarUsersResponse;
import com.example.recommender.service.UserSimilarityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Similarity", description = "Поиск пользователей со схожими интересами")
public class UserSimilarityController {

    private final UserSimilarityService userSimilarityService;

    @GetMapping("/{userId}/similar")
    @Operation(summary = "Найти пользователей с похожим поведением и оценками")
    public SimilarUsersResponse similarUsers(@PathVariable("userId") Long userId,
                                             @RequestParam(value = "limit", defaultValue = "5") @Min(1) int limit,
                                             @RequestParam(value = "period", required = false) String period,
                                             @RequestParam(value = "minOverlap", required = false) Integer minOverlap) {
        return userSimilarityService.findSimilarUsers(userId, period, limit, minOverlap);
    }
}
