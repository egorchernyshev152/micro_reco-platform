package com.example.recommender.client;

import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.MovieStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${clients.event.base-url}")
    private String baseUrl;

    private WebClient client() {
        return webClientBuilder.baseUrl(baseUrl).build();
    }

    public List<MovieStatDto> getStatsByMovie(String period) {
        return client()
                .get()
                .uri(builder -> {
                    builder.path("/events/stats/by-movie");
                    if (period != null && !period.isBlank()) {
                        builder.queryParam("period", period);
                    }
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MovieStatDto>>() {})
                .block();
    }

    public List<EventDto> getEvents(Long userId, Long movieId, String type, String period, Integer limit) {
        return client()
                .get()
                .uri(builder -> {
                    builder.path("/events");
                    if (userId != null) {
                        builder.queryParam("userId", userId);
                    }
                    if (movieId != null) {
                        builder.queryParam("movieId", movieId);
                    }
                    if (type != null && !type.isBlank()) {
                        builder.queryParam("type", type);
                    }
                    if (period != null && !period.isBlank()) {
                        builder.queryParam("period", period);
                    }
                    if (limit != null && limit > 0) {
                        builder.queryParam("limit", limit);
                    }
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<EventDto>>() {})
                .block();
    }
}
