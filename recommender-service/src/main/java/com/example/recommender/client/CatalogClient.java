package com.example.recommender.client;

import com.example.recommender.dto.MovieDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${clients.catalog.base-url}")
    private String baseUrl;

    private WebClient client() {
        return webClientBuilder.baseUrl(baseUrl).build();
    }

    public List<MovieDto> getMoviesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return client()
                .get()
                .uri(builder -> builder.path("/movies/lookup").queryParam("ids", idsParam).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MovieDto>>() {})
                .block();
    }

    public List<MovieDto> getAllMovies() {
        return client()
                .get()
                .uri(builder -> builder.path("/movies/all").build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MovieDto>>() {})
                .block();
    }

    public List<MovieDto> searchMovies(Map<String, String> params) {
        return client()
                .get()
                .uri(builder -> {
                    builder.path("/movies");
                    if (params != null) {
                        params.forEach(builder::queryParam);
                    }
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MovieDto>>() {})
                .block();
    }

    public Optional<MovieDto> getMovie(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            MovieDto movie = client()
                    .get()
                    .uri(builder -> builder.path("/movies/{id}").build(id))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(MovieDto.class)
                    .block();
            return Optional.ofNullable(movie);
        } catch (WebClientResponseException.NotFound ex) {
            return Optional.empty();
        }
    }
}
