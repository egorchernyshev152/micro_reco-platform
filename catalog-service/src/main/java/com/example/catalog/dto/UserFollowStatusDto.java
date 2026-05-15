package com.example.catalog.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserFollowStatusDto {
    long followersCount;
    long followingCount;
    boolean following;
}
