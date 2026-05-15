package com.example.catalog.dto;

import com.example.catalog.entity.MovieStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class MovieSearchRequest {
    private Set<String> genres = new LinkedHashSet<>();
    private Set<String> countries = new LinkedHashSet<>();
    private Set<String> tags = new LinkedHashSet<>();
    private Set<String> cast = new LinkedHashSet<>();
    private Set<MovieStatus> statuses = new LinkedHashSet<>();
    private Integer releaseYearFrom;
    private Integer releaseYearTo;
    private Double ratingFrom;
    private Double ratingTo;
    private Integer durationFrom;
    private Integer durationTo;
    private String query;

    /**
     * Max items returned by search endpoints (default 20, max 100).
     */
    @Min(1)
    @Max(100)
    private Integer limit = 20;

    /**
     * Page index for pagination (0-based).
     */
    @Min(0)
    private Integer page = 0;

    /**
     * Sort preset: rating_desc, rating_asc, year_desc, year_asc, created_desc.
     */
    private String sort = "rating_desc";
}
