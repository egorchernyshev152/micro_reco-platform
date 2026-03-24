package com.example.catalog.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class RatingModel {
    Long id;
    Long userId;
    Long movieId;
    Integer score;
    Instant createdAt;
}
