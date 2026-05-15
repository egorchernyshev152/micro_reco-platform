package com.example.catalog.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieDetailsResponse {
    private long id;
    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    @JsonProperty("original_language")
    private String originalLanguage;

    private String overview;
    private String tagline;
    private String status;
    private Integer runtime;

    @JsonProperty("release_date")
    private String releaseDate;

    private Long budget;
    private Long revenue;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    private List<TmdbGenreDto> genres = Collections.emptyList();

    @JsonProperty("production_countries")
    private List<TmdbProductionCountryDto> productionCountries = Collections.emptyList();

    @JsonProperty("spoken_languages")
    private List<TmdbSpokenLanguageDto> spokenLanguages = Collections.emptyList();

    @JsonProperty("credits")
    private TmdbCreditsDto credits = new TmdbCreditsDto();

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;
}
