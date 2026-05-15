package com.example.catalog.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCastDto {
    private long id;
    private String name;
    private String character;

    @JsonProperty("profile_path")
    private String profilePath;

    private Integer order;
}

