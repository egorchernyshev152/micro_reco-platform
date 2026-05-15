package com.example.catalog.dto.importer;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MovieImportResponse {
    int requestedPages;
    int processedPages;
    int importedMovies;
    int updatedMovies;
    int skippedMovies;
}
