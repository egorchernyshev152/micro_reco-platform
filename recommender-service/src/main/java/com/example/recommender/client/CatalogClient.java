package com.example.recommender.client;

import com.example.recommender.dto.ItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${clients.catalog.base-url}")
    private String baseUrl;

    public List<ItemDto> getItemsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return webClientBuilder.build()
                .get()
                .uri(baseUrl + "/items/search?ids=" + idsParam)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ItemDto>>() {})
                .block();
    }

    public List<ItemDto> getAllItems() {
        return webClientBuilder.build()
                .get()
                .uri(baseUrl + "/items")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ItemDto>>() {})
                .block();
    }

    public List<ItemDto> getItemsByCategories(Collection<String> categories) {
        if (categories == null || categories.isEmpty()) return List.of();
        String cats = categories.stream().map(s -> s.replace(",", " ")).collect(Collectors.joining(","));
        return webClientBuilder.build()
                .get()
                .uri(baseUrl + "/items/by-categories?categories=" + cats)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ItemDto>>() {})
                .block();
    }
}

