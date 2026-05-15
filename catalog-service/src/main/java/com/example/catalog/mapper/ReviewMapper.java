package com.example.catalog.mapper;

import com.example.catalog.dto.ReviewDto;
import com.example.catalog.entity.Review;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static ReviewDto toDto(Review review) {
        if (review == null) {
            return null;
        }
        return ReviewDto.builder()
                .id(review.getId())
                .movieId(review.getMovie() != null ? review.getMovie().getId() : null)
                .authorId(review.getAuthor() != null ? review.getAuthor().getId() : null)
                .authorName(review.getAuthor() != null ? review.getAuthor().getName() : null)
                .score(review.getScore())
                .content(review.getContent())
                .status(review.getStatus())
                .flagged(review.isFlagged())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
