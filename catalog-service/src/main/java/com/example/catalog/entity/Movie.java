package com.example.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", unique = true)
    private Long tmdbId;

    @Column(name = "kinopoisk_id", unique = true)
    private Long kinopoiskId;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_title")
    private String originalTitle;

    @Column(name = "original_language", length = 20)
    private String originalLanguage;

    @Column(length = 2000)
    private String description;

    @Column(length = 5000)
    private String synopsis;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "age_rating", length = 10)
    private String ageRating;

    @Column(length = 1000)
    private String tagline;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    @Builder.Default
    private MovieStatus status = MovieStatus.DRAFT;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "backdrop_url")
    private String backdropUrl;

    @Column(name = "trailer_url")
    private String trailerUrl;

    private Long budget;

    private Long revenue;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movie_genres_movie")))
    @Column(name = "genre", nullable = false)
    @Builder.Default
    private Set<String> genres = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "movie_countries",
            joinColumns = @JoinColumn(name = "movie_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movie_countries_movie")))
    @Column(name = "country", nullable = false)
    @Builder.Default
    private Set<String> countries = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "movie_tags",
            joinColumns = @JoinColumn(name = "movie_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movie_tags_movie")))
    @Column(name = "tag", nullable = false)
    @Builder.Default
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "average_rating")
    private Double averageRating;

    @Column(name = "ratings_count")
    private Long ratingsCount;

    @Column(name = "imported_rating")
    private Double importedRating;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "movie_cast",
            joinColumns = @JoinColumn(name = "movie_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movie_cast_movie")))
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<MovieCastMember> cast = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
