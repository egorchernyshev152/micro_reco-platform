package com.example.catalog.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class UserFollowListItemDto {
    Long id;
    String name;
    boolean profilePrivate;
    boolean mutual;
    Instant followedAt;
}
