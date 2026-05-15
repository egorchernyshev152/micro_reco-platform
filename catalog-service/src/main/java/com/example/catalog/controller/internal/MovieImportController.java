package com.example.catalog.controller.internal;

import com.example.catalog.dto.importer.MovieImportResponse;
import com.example.catalog.dto.importer.TmdbImportRequest;
import com.example.catalog.service.importer.MovieImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/import")
@RequiredArgsConstructor
public class MovieImportController {

    private final MovieImportService movieImportService;

    @PostMapping("/tmdb")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public MovieImportResponse importFromTmdb(@Valid @RequestBody TmdbImportRequest request) {
        return movieImportService.importPopularMovies(request);
    }
}
