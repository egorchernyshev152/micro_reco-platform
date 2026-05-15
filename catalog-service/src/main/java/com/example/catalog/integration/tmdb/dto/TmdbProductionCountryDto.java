package com.example.catalog.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbProductionCountryDto {
    @JsonProperty("iso_3166_1")
    private String isoCode;
    private String name;
}
