package com.example.catalog.controller;

import com.example.catalog.dto.MovieAssetDto;
import com.example.catalog.dto.MovieDto;
import com.example.catalog.dto.MoviePageResponse;
import com.example.catalog.dto.MovieSearchRequest;
import com.example.catalog.entity.MovieAssetType;
import com.example.catalog.service.MovieAssetService;
import com.example.catalog.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
@Validated
@Tag(name = "Movies", description = "Movie catalog operations")
public class MovieController {
    private final MovieService movieService;
    private final MovieAssetService movieAssetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create movie")
    @PreAuthorize("hasRole('ADMIN')")
    public MovieDto create(@Valid @RequestBody MovieDto dto) {
        return movieService.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update movie")
    @PreAuthorize("hasRole('ADMIN')")
    public MovieDto update(@PathVariable("id") Long id, @Valid @RequestBody MovieDto dto) {
        return movieService.update(id, dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie by id")
    public MovieDto get(@PathVariable("id") Long id) {
        return movieService.get(id);
    }

    @GetMapping
    @Operation(summary = "Search movies with filters")
    public MoviePageResponse search(@Valid @ModelAttribute MovieSearchRequest request) {
        return movieService.search(request);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all movies without filters")
    public List<MovieDto> getAll() {
        return movieService.getAll();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete movie")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable("id") Long id) {
        movieService.delete(id);
    }

    @GetMapping("/lookup")
    @Operation(summary = "Find movies by ids")
    public List<MovieDto> findByIds(@RequestParam("ids") Collection<Long> ids) {
        return movieService.findByIds(ids);
    }

    @PostMapping(path = "/{id}/assets", consumes = {"multipart/form-data"})
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload asset for movie")
    @PreAuthorize("hasRole('ADMIN')")
    public MovieAssetDto uploadAsset(@PathVariable("id") Long id,
                                     @RequestPart("file") MultipartFile file,
                                     @RequestParam(name = "type", required = false) MovieAssetType type,
                                     @RequestParam(name = "label", required = false) String label) {
        return movieAssetService.upload(id, type, file, label);
    }
}
