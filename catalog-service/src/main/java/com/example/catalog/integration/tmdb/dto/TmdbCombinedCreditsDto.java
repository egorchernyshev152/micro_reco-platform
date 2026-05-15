package com.example.catalog.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCombinedCreditsDto {
    private List<TmdbCreditDto> cast = Collections.emptyList();
}
