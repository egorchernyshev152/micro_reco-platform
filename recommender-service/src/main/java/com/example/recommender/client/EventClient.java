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

    public List<ItemStatDto> getStatsByItem(String period) {
        String url = baseUrl + "/events/stats/by-item";
        if (period != null && !period.isBlank()) {
            url += "?period=" + period;
        }
        return webClientBuilder.build()
                .get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ItemStatDto>>() {})
                .block();
    }

    public List<EventDto> getEventsByUser(Long userId) {
        String url = baseUrl + "/events?userId=" + userId;
        return webClientBuilder.build()
                .get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<EventDto>>() {})
                .block();
    }
}

