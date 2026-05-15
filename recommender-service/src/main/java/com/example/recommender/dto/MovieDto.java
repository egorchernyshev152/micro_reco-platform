package com.example.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDto {
    private Long id;
    private String title;
    private String originalTitle;
    private String description;
    private String synopsis;
    private Integer releaseYear;
    private LocalDate releaseDate;
    private Integer durationMinutes;
    private String ageRating;
    private String tagline;
    private String status;
    private String posterUrl;
    private String backdropUrl;
    private String trailerUrl;
    private Long budget;
    private Long revenue;
    @Builder.Default
    private Set<String> genres = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> countries = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> tags = new LinkedHashSet<>();
    private Double averageRating;
    private Long ratingsCount;
}

