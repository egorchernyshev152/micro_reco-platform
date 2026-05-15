package com.example.catalog.controller.admin;

import com.example.catalog.dto.admin.AdminReviewBulkActionRequest;
import com.example.catalog.dto.admin.AdminReviewPageResponse;
import com.example.catalog.dto.admin.AdminReviewResponse;
import com.example.catalog.dto.admin.AdminReviewStatusRequest;
import com.example.catalog.entity.ReviewStatus;
import com.example.catalog.security.UserPrincipal;
import com.example.catalog.service.admin.AdminReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/admin/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin Reviews", description = "Модерация отзывов пользователей")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Поиск отзывов с фильтрами для модерации")
    public AdminReviewPageResponse list(
            @Parameter(description = "Поиск по тексту отзыва, фильму или автору")
            @RequestParam(value = "query", required = false) String query,
            @Parameter(description = "Фильтр по фильму") @RequestParam(value = "movieId", required = false) Long movieId,
            @Parameter(description = "Фильтр по пользователю") @RequestParam(value = "userId", required = false) Long userId,
            @Parameter(description = "Статус отзыва") @RequestParam(value = "status", required = false) ReviewStatus status,
            @Parameter(description = "Флагованные отзывы") @RequestParam(value = "flagged", required = false) Boolean flagged,
            @Parameter(description = "Номер страницы") @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Размер страницы") @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Сортировка") @RequestParam(value = "sort", required = false) String sort
    ) {
        return adminReviewService.search(query, movieId, userId, status, flagged, page, size, sort);
    }

    @PatchMapping(path = "/{reviewId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Изменить статус отдельного отзыва")
    public AdminReviewResponse updateStatus(@PathVariable("reviewId") Long reviewId,
                                            @Valid @RequestBody AdminReviewStatusRequest request,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return adminReviewService.updateStatus(reviewId, request.getStatus(), request.getReason(), principal);
    }

    @PostMapping(path = "/bulk-action", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Групповая модерация отзывов")
    public void bulkAction(@Valid @RequestBody AdminReviewBulkActionRequest request,
                           @AuthenticationPrincipal UserPrincipal principal) {
        adminReviewService.bulkAction(request, principal);
    }
}
