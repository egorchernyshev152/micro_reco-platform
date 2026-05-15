package com.example.catalog.controller;

import com.example.catalog.dto.MovieDto;
import com.example.catalog.dto.UserMovieCollectionSummaryDto;
import com.example.catalog.entity.UserMovieCollectionType;
import com.example.catalog.service.UserMovieCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/collections")
@RequiredArgsConstructor
@Tag(name = "User Collections", description = "Favorites, watchlist and watched movies")
public class UserCollectionController {

    private final UserMovieCollectionService collectionService;

    @PostMapping("/{type}/{movieId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Add movie to user collection")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public void add(@PathVariable("userId") @Min(1) Long userId,
                    @PathVariable("movieId") @Min(1) Long movieId,
                    @PathVariable("type") UserMovieCollectionType type) {
        collectionService.add(userId, movieId, type);
    }

    @DeleteMapping("/{type}/{movieId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove movie from user collection")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public void remove(@PathVariable("userId") @Min(1) Long userId,
                       @PathVariable("movieId") @Min(1) Long movieId,
                       @PathVariable("type") UserMovieCollectionType type) {
        collectionService.remove(userId, movieId, type);
    }

    @GetMapping("/{type}")
    @Operation(summary = "Get user collection")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public List<MovieDto> list(@PathVariable("userId") @Min(1) Long userId,
                               @PathVariable("type") UserMovieCollectionType type) {
        return collectionService.getCollection(userId, type);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get collection summary for movies")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public List<UserMovieCollectionSummaryDto> summary(@PathVariable("userId") @Min(1) Long userId,
                                                       @RequestParam("movieIds") List<Long> movieIds) {
        return collectionService.getSummary(userId, movieIds);
    }
}
