package com.example.catalog.controller;

import com.example.catalog.dto.ActorDetailsDto;
import com.example.catalog.service.ActorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actors")
@RequiredArgsConstructor
@Validated
@Tag(name = "Actors", description = "Actor info sourced from TMDb")
public class ActorController {

    private final ActorService actorService;

    @GetMapping("/{tmdbId}")
    @Operation(summary = "Get actor details by TMDb id")
    public ActorDetailsDto getActorDetails(@PathVariable("tmdbId") Long tmdbId,
                                           @RequestParam(name = "language", required = false) String language) {
        return actorService.getActorDetails(tmdbId, language);
    }
}
