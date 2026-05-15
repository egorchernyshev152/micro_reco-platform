package com.example.catalog.dto;

import com.example.catalog.entity.MovieStatus;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CatalogFiltersDto {
    List<String> genres;
    List<String> countries;
    List<String> tags;
    List<MovieStatus> statuses;
    Integer minYear;
    Integer maxYear;
}
