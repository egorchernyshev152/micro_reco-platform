package com.example.catalog.dto;

import com.example.catalog.entity.ReviewStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ReviewDto {
    Long id;
    Long movieId;
    Long authorId;
    String authorName;
    Integer score;
    String content;
    ReviewStatus status;
    boolean flagged;
    Instant createdAt;
    Instant updatedAt;
}
