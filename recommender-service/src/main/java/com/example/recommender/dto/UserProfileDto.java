package com.example.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String name;
    private boolean profilePrivate;
    private Instant createdAt;
    private Instant updatedAt;
    private Long favoritesCount;
    private Long watchlistCount;
    private Long watchedCount;
}
