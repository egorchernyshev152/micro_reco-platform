package com.example.catalog.service;

import com.example.catalog.dto.CatalogFiltersDto;
import com.example.catalog.entity.MovieStatus;
import com.example.catalog.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class CatalogMetadataService {

    private final MovieRepository movieRepository;

    public CatalogFiltersDto getFilters() {
        return CatalogFiltersDto.builder()
                .genres(movieRepository.findDistinctGenres())
                .countries(movieRepository.findDistinctCountries())
                .tags(movieRepository.findDistinctTags())
                .statuses(Arrays.stream(MovieStatus.values()).toList())
                .minYear(movieRepository.findMinReleaseYear())
                .maxYear(movieRepository.findMaxReleaseYear())
                .build();
    }
}
