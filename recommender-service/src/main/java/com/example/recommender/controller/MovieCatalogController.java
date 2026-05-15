package com.example.recommender.controller;

import com.example.recommender.client.CatalogClient;
import com.example.recommender.dto.MovieDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
@Tag(name = "Movie Catalog Proxy", description = "Прозрачная прокси-выдача каталога для фронтенда")
public class MovieCatalogController {

    private final CatalogClient catalogClient;

    @GetMapping
    @Operation(summary = "Поиск фильмов через catalog-service")
    public List<MovieDto> search(@RequestParam Map<String, String> params) {
        return catalogClient.searchMovies(params);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить карточку фильма")
    public MovieDto get(@PathVariable Long id) {
        return catalogClient.getMovie(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found: " + id));
    }
}
