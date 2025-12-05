package com.example.recommender.client;

import com.example.recommender.dto.EventDto;
import com.example.recommender.dto.ItemStatDto;
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

    public List<ItemStatDto> getStatsByItem(String period) {
        return client()
                .get()
                .uri(builder -> {
                    var uri = builder.path("/events/stats/by-item");
                    if (period != null && !period.isBlank()) {
                        uri.queryParam("period", period);
                    }
                    return uri.build();
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ItemStatDto>>() {})
                .block();
    }

    public List<EventDto> getEventsByUser(Long userId) {
        return client()
                .get()
                .uri(builder -> builder.path("/events").queryParam("userId", userId).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<EventDto>>() {})
                .block();
    }
}

