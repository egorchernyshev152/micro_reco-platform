package com.example.recommender.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(WebClientConfig.WebClientProperties.class)
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder(WebClientProperties props, com.example.recommender.security.JwtService jwtService) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) props.connectTimeout().toMillis())
                .responseTimeout(props.responseTimeout())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(props.readTimeout().toMillis(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(props.readTimeout().toMillis(), TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter((request, next) -> {
                    String token = jwtService.createServiceToken("recommender-service");
                    ClientRequest withAuth = ClientRequest.from(request)
                            .headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                            .build();
                    return next.exchange(withAuth);
                })
                .filter((request, next) -> next.exchange(request).flatMap(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new WebClientResponseException(
                                        "Downstream call failed: " + response.statusCode(),
                                        response.statusCode().value(),
                                        response.statusCode().toString(),
                                        response.headers().asHttpHeaders(),
                                        body.getBytes(),
                                        null
                                )));
                    }
                    return Mono.just(response);
                }))
                .filter((request, next) -> next.exchange(request).doOnSubscribe(sub -> {
                    // lightweight per-request log
                    System.out.printf("HTTP %s %s%n", request.method(), request.url());
                }));
    }

    /**
     * Lightweight properties holder for WebClient timeouts.
     */
    @ConfigurationProperties(prefix = "clients.http")
    public static class WebClientProperties {
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(5);
        private Duration responseTimeout = Duration.ofSeconds(5);

        public Duration connectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration readTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Duration responseTimeout() {
            return responseTimeout;
        }

        public void setResponseTimeout(Duration responseTimeout) {
            this.responseTimeout = responseTimeout;
        }
    }
}

