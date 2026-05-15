package com.example.catalog.integration.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final WebClient.Builder webClientBuilder;

    @Value("${clients.event.base-url:http://localhost:8082}")
    private String baseUrl;

    @Value("${clients.event.source:CATALOG}")
    private String source;

    public void publishCollectionEvent(Long userId, Long movieId, String eventType) {
        publish(EventRequest.builder()
                .userId(userId)
                .movieId(movieId)
                .type(eventType)
                .source(source)
                .device("web")
                .build());
    }

    public void publishRatingEvent(Long userId, Long movieId, int score) {
        publish(EventRequest.builder()
                .userId(userId)
                .movieId(movieId)
                .type("RATE")
                .source(source)
                .device("web")
                .payload(Map.of("score", score))
                .build());
    }

    public void publishActorFavoriteEvent(Long userId, Long actorId, String actorName) {
        publish(EventRequest.builder()
                .userId(userId)
                .actorId(actorId)
                .actorName(actorName)
                .type("FAVORITE_ACTOR")
                .source(source)
                .device("web")
                .build());
    }

    private void publish(EventRequest request) {
        if (request.getUserId() == null || !StringUtils.hasText(request.getType())) {
            return;
        }
        webClientBuilder.baseUrl(baseUrl).build()
                .post()
                .uri("/events")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(3))
                .doOnError(ex -> log.warn("Не удалось отправить событие {} от пользователя {}: {}",
                        request.getType(), request.getUserId(), ex.getMessage()))
                .onErrorResume(ex -> Mono.empty())
                .subscribe();
    }
}
