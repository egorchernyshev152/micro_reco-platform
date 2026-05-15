package com.example.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarUserDto {
    private Long userId;
    private double similarity;
    private int sharedMovies;
    private List<Long> sharedMovieIds;
    private UserProfileDto profile;
}
