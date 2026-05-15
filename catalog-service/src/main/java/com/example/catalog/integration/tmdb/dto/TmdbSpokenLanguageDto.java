package com.example.catalog.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbSpokenLanguageDto {
    @JsonProperty("iso_639_1")
    private String isoCode;
    private String name;
}
