package com.example.catalog.controller;

import com.example.catalog.dto.PageResponse;
import com.example.catalog.dto.ReviewDto;
import com.example.catalog.dto.ReviewRequest;
import com.example.catalog.security.UserPrincipal;
import com.example.catalog.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movies/{movieId}/reviews")
@RequiredArgsConstructor
@Validated
@Tag(name = "Movie Reviews", description = "Отзывы зрителей о фильмах")
public class MovieReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "Список опубликованных отзывов по фильму")
    public PageResponse<ReviewDto> list(@PathVariable("movieId") @Min(1) Long movieId,
                                        @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
                                        @RequestParam(value = "size", defaultValue = "6") @Min(1) @Max(50) int size) {
        return reviewService.getPublishedReviews(movieId, page, size);
    }

    @GetMapping("/my")
    @Operation(summary = "Текущий отзыв пользователя на фильм")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDto> myReview(@PathVariable("movieId") @Min(1) Long movieId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        ReviewDto review = reviewService.getUserReview(principal.getId(), movieId);
        if (review == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(review);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Оставить отзыв на фильм (попадает в очередь модерации)")
    @PreAuthorize("isAuthenticated()")
    public ReviewDto submit(@PathVariable("movieId") @Min(1) Long movieId,
                            @AuthenticationPrincipal UserPrincipal principal,
                            @Valid @RequestBody ReviewRequest request) {
        return reviewService.submitReview(principal.getId(), movieId, request);
    }
}
