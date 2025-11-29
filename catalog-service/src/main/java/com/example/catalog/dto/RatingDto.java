package com.example.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingDto {
    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    private Long itemId;

    @Min(1)
    @Max(5)
    private Integer score;

    private Instant createdAt;
}

