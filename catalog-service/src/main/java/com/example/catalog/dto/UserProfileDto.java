package com.example.catalog.dto;

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
    private long favoritesCount;
    private long watchlistCount;
    private long watchedCount;
    private long followersCount;
    private long followingCount;
}
