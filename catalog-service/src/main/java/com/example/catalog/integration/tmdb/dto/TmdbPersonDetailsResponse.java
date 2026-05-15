package com.example.catalog.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbPersonDetailsResponse {
    private long id;
    private String name;
    private String biography;
    private String birthday;
    private String deathday;

    @JsonProperty("place_of_birth")
    private String placeOfBirth;

    @JsonProperty("profile_path")
    private String profilePath;

    @JsonProperty("known_for_department")
    private String knownForDepartment;

    private Double popularity;

    @JsonProperty("also_known_as")
    private List<String> alsoKnownAs = Collections.emptyList();

    @JsonProperty("combined_credits")
    private TmdbCombinedCreditsDto combinedCredits = new TmdbCombinedCreditsDto();
}
