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

    private WebClient client() {
        // собираем WebClient с базовым url каталога, настроенным в application.yml
        return webClientBuilder.baseUrl(baseUrl).build();
    }

    public List<ItemDto> getItemsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        // GET /items/search (catalog-service) — подтягиваем карточки товаров по id
        return client()
                .get()
                .uri(builder -> builder.path("/items/search").queryParam("ids", idsParam).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ItemDto>>() {})
                .block();
    }

    public List<ItemDto> getAllItems() {
        // GET /items (catalog-service) — полный список товаров
        return client()
                .get()
                .uri(builder -> builder.path("/items").build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ItemDto>>() {})
                .block();
    }

    public List<ItemDto> getItemsByCategories(Collection<String> categories) {
        if (categories == null || categories.isEmpty()) return List.of();
        String cats = categories.stream().map(s -> s.replace(",", " ")).collect(Collectors.joining(","));
        // GET /items/by-categories (catalog-service) — кандидаты по нужным категориям
        return client()
                .get()
                .uri(builder -> builder.path("/items/by-categories").queryParam("categories", cats).build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ItemDto>>() {})
                .block();
    }
}
