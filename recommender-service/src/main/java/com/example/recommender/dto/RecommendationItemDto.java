package com.example.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationItemDto {
    private MovieDto movie;
    private double score;
    private Double popularityScore;
    private Double contentScore;
}
