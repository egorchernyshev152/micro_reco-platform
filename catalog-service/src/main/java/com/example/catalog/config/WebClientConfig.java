package com.example.catalog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

@Configuration
@Slf4j
public class WebClientConfig {

    @Bean
    public WebClient tmdbWebClient(WebClient.Builder builder, TmdbProperties properties) {
        HttpClient httpClient = HttpClient.create();

        if (properties.isProxyEnabled()) {
            TmdbProperties.ProxySettings proxy = properties.getProxy();
            log.info("TMDb WebClient использует прокси {}:{}", proxy.getHost(), proxy.getPort());
            httpClient = httpClient.proxy(proxySpec -> proxySpec.type(ProxyProvider.Proxy.HTTP)
                    .host(proxy.getHost())
                    .port(proxy.getPort()));
        } else {
            log.info("TMDb WebClient использует системные настройки прокси (http[s].proxyHost)");
            httpClient = httpClient.proxyWithSystemProperties();
        }

        return builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }
}
