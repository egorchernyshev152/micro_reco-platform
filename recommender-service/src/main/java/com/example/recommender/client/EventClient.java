package com.example.recommender.client;

import com.example.recommender.dto.DayStatDto;
import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.MovieStatDto;
import com.example.recommender.dto.TimeDistributionStatDto;
import com.example.recommender.dto.UserStatDto;
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

    public List<EventDto> getEvents(Long userId,
                                    Long movieId,
                                    String type,
                                    String period,
                                    Integer limit,
                                    String source) {
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
                    if (source != null && !source.isBlank()) {
                        builder.queryParam("source", source);
                    }
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<EventDto>>() {})
                .block();
    }

    public List<UserStatDto> getUserStats(String period) {
        return client()
                .get()
                .uri(builder -> {
                    builder.path("/events/stats/by-user");
                    if (period != null && !period.isBlank()) {
                        builder.queryParam("period", period);
                    }
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UserStatDto>>() {})
                .block();
    }

    public List<DayStatDto> getDailyStats(String period) {
        return client()
                .get()
                .uri(builder -> {
                    builder.path("/events/stats/by-day");
                    if (period != null && !period.isBlank()) {
                        builder.queryParam("period", period);
                    }
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<DayStatDto>>() {})
                .block();
    }

    public List<TimeDistributionStatDto> getTimeDistribution(String period) {
        return client()
                .get()
                .uri(builder -> {
                    builder.path("/events/stats/time-distribution");
                    if (period != null && !period.isBlank()) {
                        builder.queryParam("period", period);
                    }
                    return builder.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TimeDistributionStatDto>>() {})
                .block();
    }
}
