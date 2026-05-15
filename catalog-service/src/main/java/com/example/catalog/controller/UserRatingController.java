package com.example.catalog.controller;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.dto.RatingRequest;
import com.example.catalog.security.UserPrincipal;
import com.example.catalog.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movies/{movieId}/rating")
@RequiredArgsConstructor
@Tag(name = "User Ratings", description = "Оценки фильмов от текущего пользователя")
public class UserRatingController {

    private final RatingService ratingService;

    @GetMapping
    @Operation(summary = "Получить свою оценку фильма")
    public ResponseEntity<RatingDto> get(@PathVariable("movieId") @Min(1) Long movieId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        RatingDto rating = ratingService.getUserRating(principal.getId(), movieId);
        if (rating == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(rating);
    }

    @PostMapping
    @Operation(summary = "Поставить или обновить оценку фильма (1-10)")
    public RatingDto upsert(@PathVariable("movieId") @Min(1) Long movieId,
                            @AuthenticationPrincipal UserPrincipal principal,
                            @Valid @RequestBody RatingRequest request) {
        return ratingService.setUserRating(principal.getId(), movieId, request.getScore());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить свою оценку фильма")
    public void delete(@PathVariable("movieId") @Min(1) Long movieId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        ratingService.deleteUserRating(principal.getId(), movieId);
    }
}
