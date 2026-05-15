package com.example.catalog.dto;

import com.example.catalog.entity.MovieStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieDto {
    private Long id;

    @NotBlank
    private String title;

    private String originalTitle;
    private String originalLanguage;
    private String description;
    private String synopsis;
    private Integer releaseYear;
    private LocalDate releaseDate;
    private Integer durationMinutes;
    private String ageRating;
    private String tagline;
    @Builder.Default
    private MovieStatus status = MovieStatus.DRAFT;
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
    private Double importedRating;

    @Builder.Default
    private List<CastMemberDto> cast = new ArrayList<>();
}
