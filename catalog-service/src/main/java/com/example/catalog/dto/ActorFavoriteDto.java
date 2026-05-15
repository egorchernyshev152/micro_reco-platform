package com.example.catalog.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ActorFavoriteDto {
    Long actorTmdbId;
    String actorName;
    String profileUrl;
    Instant createdAt;
}
