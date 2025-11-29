package com.example.recommender.controller;

import com.example.recommender.dto.ItemDto;
import com.example.recommender.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendations", description = "Recommendation endpoints")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/popular")
    @Operation(summary = "Popular recommendations")
    public List<ItemDto> popular(@RequestParam(value = "period", required = false) String period,
                                 @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit) {
        return recommendationService.popularRecommendations(period, limit);
    }

    @GetMapping("/personalized/{userId}")
    @Operation(summary = "Personalized recommendations")
    public List<ItemDto> personalized(@PathVariable("userId") Long userId,
                                      @RequestParam(value = "limit", defaultValue = "10") @Min(1) int limit) {
        return recommendationService.personalizedRecommendations(userId, limit);
    }
}
