package com.example.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActorKnownForDto {
    private String title;
    private Integer year;
    private Double voteAverage;
    private Double catalogRating;
    private Long tmdbId;
    private Long movieId;
    private String mediaType;
    private String character;
}
