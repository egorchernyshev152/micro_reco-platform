package com.example.catalog.dto.admin;

import com.example.catalog.entity.ReviewStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class AdminReviewResponse {
    Long id;
    Long movieId;
    String movieTitle;
    Long userId;
    String userName;
    String userEmail;
    Integer score;
    String content;
    ReviewStatus status;
    boolean flagged;
    String lastModerationReason;
    String moderatedBy;
    Instant moderatedAt;
    Instant createdAt;
    Instant updatedAt;
}
