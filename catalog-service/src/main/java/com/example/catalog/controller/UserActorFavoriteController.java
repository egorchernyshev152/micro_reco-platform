package com.example.catalog.controller;

import com.example.catalog.dto.ActorFavoriteDto;
import com.example.catalog.dto.ActorFavoriteRequest;
import com.example.catalog.service.UserActorFavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/favorite-actors")
@RequiredArgsConstructor
@Tag(name = "Favorite Actors", description = "User favorite actors management")
public class UserActorFavoriteController {

    private final UserActorFavoriteService favoriteService;

    @PostMapping("/{actorTmdbId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add actor to favorites")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public ActorFavoriteDto add(@PathVariable("userId") @Min(1) Long userId,
                                @PathVariable("actorTmdbId") @Min(1) Long actorTmdbId,
                                @RequestBody @Valid ActorFavoriteRequest request) {
        return favoriteService.add(userId, actorTmdbId, request);
    }

    @DeleteMapping("/{actorTmdbId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove actor from favorites")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public void remove(@PathVariable("userId") @Min(1) Long userId,
                       @PathVariable("actorTmdbId") @Min(1) Long actorTmdbId) {
        favoriteService.remove(userId, actorTmdbId);
    }

    @GetMapping
    @Operation(summary = "List favorite actors")
    @PreAuthorize("@userSecurity.isOwnerOrAdmin(#userId)")
    public List<ActorFavoriteDto> list(@PathVariable("userId") @Min(1) Long userId) {
        return favoriteService.list(userId);
    }
}
