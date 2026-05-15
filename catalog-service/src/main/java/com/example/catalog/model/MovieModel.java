package com.example.catalog.model;

import com.example.catalog.entity.MovieStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class MovieModel {
    Long id;
    String title;
    String originalTitle;
    String originalLanguage;
    String description;
    String synopsis;
    Integer releaseYear;
    LocalDate releaseDate;
    Integer durationMinutes;
    String ageRating;
    String tagline;
    @Builder.Default
    MovieStatus status = MovieStatus.DRAFT;
    String posterUrl;
    String backdropUrl;
    String trailerUrl;
    Long budget;
    Long revenue;
    Set<String> genres;
    Set<String> countries;
    Set<String> tags;
    Double averageRating;
    Long ratingsCount;
    Double importedRating;
    @Builder.Default
    List<CastMemberModel> cast = new ArrayList<>();
}
