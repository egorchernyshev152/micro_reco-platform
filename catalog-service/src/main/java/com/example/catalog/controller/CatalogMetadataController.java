package com.example.catalog.controller;

import com.example.catalog.dto.CatalogFiltersDto;
import com.example.catalog.service.CatalogMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog metadata")
public class CatalogMetadataController {

    private final CatalogMetadataService metadataService;

    @GetMapping("/filters")
    @Operation(summary = "Available filters: genres, countries, tags, years")
    public CatalogFiltersDto filters() {
        return metadataService.getFilters();
    }
}

