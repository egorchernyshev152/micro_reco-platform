package com.example.catalog.service.admin;

import com.example.catalog.dto.admin.AdminReviewBulkActionRequest;
import com.example.catalog.dto.admin.AdminReviewPageResponse;
import com.example.catalog.dto.admin.AdminReviewResponse;
import com.example.catalog.dto.admin.AdminReviewStatsResponse;
import com.example.catalog.entity.Review;
import com.example.catalog.entity.ReviewModerationAction;
import com.example.catalog.entity.ReviewModerationLog;
import com.example.catalog.entity.ReviewStatus;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.ReviewModerationLogRepository;
import com.example.catalog.repository.ReviewRepository;
import com.example.catalog.repository.spec.ReviewSpecifications;
import com.example.catalog.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewModerationLogRepository reviewModerationLogRepository;

    @Transactional(readOnly = true)
    public AdminReviewPageResponse search(String query,
                                          Long movieId,
                                          Long userId,
                                          ReviewStatus status,
                                          Boolean flagged,
                                          int page,
                                          int size,
                                          String sort) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        Specification<Review> base = ReviewSpecifications.baseFilters(query, movieId, userId, flagged);
        Specification<Review> spec = ReviewSpecifications.withStatus(base, status);
        Page<Review> result = reviewRepository.findAll(spec, pageable);
        AdminReviewStatsResponse stats = buildStats(base);
        return AdminReviewPageResponse.builder()
                .items(result.getContent().stream().map(this::mapReview).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .hasNext(result.hasNext())
                .stats(stats)
                .build();
    }

    @Transactional
    public AdminReviewResponse updateStatus(Long reviewId, ReviewStatus status, String reason, UserPrincipal principal) {
        if (status == null) {
            throw new IllegalArgumentException("Статус обязателен");
        }
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found: " + reviewId));
        if (review.getStatus() == status) {
            return mapReview(review);
        }
        applyStatus(review, status, reason, principal);
        Review saved = reviewRepository.save(review);
        recordModeration(saved, status, reason, principal);
        return mapReview(saved);
    }

    @Transactional
    public void bulkAction(AdminReviewBulkActionRequest request, UserPrincipal principal) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            throw new IllegalArgumentException("Не выбраны отзывы для модерации");
        }
        List<Review> reviews = reviewRepository.findAllById(request.getIds());
        if (reviews.size() != request.getIds().size()) {
            throw new NotFoundException("Некоторые отзывы не найдены");
        }
        List<Review> changed = new ArrayList<>();
        for (Review review : reviews) {
            if (review.getStatus() == request.getStatus()) {
                continue;
            }
            applyStatus(review, request.getStatus(), request.getReason(), principal);
            changed.add(review);
        }
        if (changed.isEmpty()) {
            return;
        }
        reviewRepository.saveAll(changed);
        changed.forEach(review -> recordModeration(review, request.getStatus(), request.getReason(), principal));
    }

    private void applyStatus(Review review, ReviewStatus status, String reason, UserPrincipal principal) {
        Instant now = Instant.now();
        review.setStatus(status);
        review.setLastModerationReason(defaultReason(reason));
        review.setModeratedAt(now);
        review.setModeratedBy(actorLabel(principal));
        if (status == ReviewStatus.PUBLISHED) {
            review.setFlagged(false);
        } else if (status == ReviewStatus.SPAM) {
            review.setFlagged(true);
            if (review.getFlaggedAt() == null) {
                review.setFlaggedAt(now);
            }
        } else if (status == ReviewStatus.PENDING) {
            review.setFlagged(false);
            review.setFlaggedAt(null);
        }
    }

    private void recordModeration(Review review, ReviewStatus status, String reason, UserPrincipal principal) {
        ReviewModerationLog log = ReviewModerationLog.builder()
                .review(review)
                .action(actionForStatus(status))
                .status(status)
                .reason(defaultReason(reason))
                .performedById(principal != null ? principal.getId() : null)
                .performedByEmail(principal != null ? principal.getUsername() : "system")
                .performedByName(principal != null ? principal.getUsername() : "system")
                .build();
        reviewModerationLogRepository.save(log);
    }

    private ReviewModerationAction actionForStatus(ReviewStatus status) {
        return switch (status) {
            case SPAM -> ReviewModerationAction.MARK_SPAM;
            case DELETED -> ReviewModerationAction.DELETE;
            case PUBLISHED -> ReviewModerationAction.RESTORE;
            case PENDING -> ReviewModerationAction.RESET_TO_PENDING;
        };
    }

    private AdminReviewResponse mapReview(Review review) {
        return AdminReviewResponse.builder()
                .id(review.getId())
                .movieId(review.getMovie().getId())
                .movieTitle(review.getMovie().getTitle())
                .userId(review.getAuthor().getId())
                .userName(review.getAuthor().getName())
                .userEmail(review.getAuthor().getEmail())
                .score(review.getScore())
                .content(review.getContent())
                .status(review.getStatus())
                .flagged(review.isFlagged())
                .lastModerationReason(review.getLastModerationReason())
                .moderatedAt(review.getModeratedAt())
                .moderatedBy(review.getModeratedBy())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private AdminReviewStatsResponse buildStats(Specification<Review> baseSpec) {
        long total = reviewRepository.count(baseSpec);
        long pending = reviewRepository.count(baseSpec.and(ReviewSpecifications.hasStatus(ReviewStatus.PENDING)));
        long published = reviewRepository.count(baseSpec.and(ReviewSpecifications.hasStatus(ReviewStatus.PUBLISHED)));
        long spam = reviewRepository.count(baseSpec.and(ReviewSpecifications.hasStatus(ReviewStatus.SPAM)));
        long deleted = reviewRepository.count(baseSpec.and(ReviewSpecifications.hasStatus(ReviewStatus.DELETED)));
        long flagged = reviewRepository.count(baseSpec.and(ReviewSpecifications.isFlagged(true)));
        return AdminReviewStatsResponse.builder()
                .total(total)
                .pending(pending)
                .published(published)
                .spam(spam)
                .deleted(deleted)
                .flagged(flagged)
                .build();
    }

    private Sort resolveSort(String sortParam) {
        if (!StringUtils.hasText(sortParam)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] tokens = sortParam.split(",");
        String property = tokens[0].trim();
        Sort.Direction direction = tokens.length > 1 ? Sort.Direction.fromString(tokens[1].trim()) : Sort.Direction.ASC;
        if (!StringUtils.hasText(property)) {
            property = "createdAt";
        }
        return Sort.by(direction, property);
    }

    private String actorLabel(UserPrincipal principal) {
        return principal != null ? principal.getUsername() : "system";
    }

    private String defaultReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "Без указания причины";
        }
        return reason;
    }
}
