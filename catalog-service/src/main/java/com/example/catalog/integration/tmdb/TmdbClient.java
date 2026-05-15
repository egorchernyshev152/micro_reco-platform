package com.example.catalog.integration.tmdb;

import com.example.catalog.config.TmdbProperties;
import com.example.catalog.dto.importer.TmdbImportRequest;
import com.example.catalog.integration.tmdb.dto.TmdbDiscoverResponse;
import com.example.catalog.integration.tmdb.dto.TmdbMovieDetailsResponse;
import com.example.catalog.integration.tmdb.dto.TmdbPersonDetailsResponse;
import com.example.catalog.integration.tmdb.exception.TmdbClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TmdbClient {

    private final WebClient tmdbWebClient;
    private final TmdbProperties properties;

    public TmdbDiscoverResponse discoverMovies(int page, TmdbImportRequest request, String originCountry) {
        log.debug("Запрашиваем популярные фильмы TMDb: page={}, lang={}, originCountry={}",
                page, request.getLanguage(), originCountry);

        return tmdbWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/discover/movie")
                            .queryParam("api_key", properties.getApiKey())
                            .queryParam("sort_by", "popularity.desc")
                            .queryParam("include_adult", request.isIncludeAdult())
                            .queryParam("language", request.getLanguage())
                            .queryParam("page", page);

                    if (StringUtils.hasText(originCountry)) {
                        uriBuilder.queryParam("with_origin_country", originCountry);
                    }
                    if (StringUtils.hasText(request.getOriginalLanguage())) {
                        uriBuilder.queryParam("with_original_language", request.getOriginalLanguage());
                    }
                    if (request.getYearFrom() != null) {
                        uriBuilder.queryParam("primary_release_date.gte", request.getYearFrom() + "-01-01");
                    }
                    if (request.getYearTo() != null) {
                        uriBuilder.queryParam("primary_release_date.lte", request.getYearTo() + "-12-31");
                    }
                    if (request.getMinVoteAverage() != null) {
                        uriBuilder.queryParam("vote_average.gte", request.getMinVoteAverage());
                    }
                    if (request.getMinVoteCount() != null) {
                        uriBuilder.queryParam("vote_count.gte", request.getMinVoteCount());
                    }
                    if (!CollectionUtils.isEmpty(request.getGenreIds())) {
                        String value = request.getGenreIds().stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(","));
                        uriBuilder.queryParam("with_genres", value);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .doOnNext(body -> log.error("TMDb discover API error {}: {}", response.statusCode(), body))
                        .map(body -> new TmdbClientException("Ошибка TMDb discover API: " + body)))
                .bodyToMono(TmdbDiscoverResponse.class)
                .block();
    }

    public TmdbMovieDetailsResponse getMovieDetails(long movieId, String language) {
        log.debug("Запрашиваем детали фильма TMDb: id={}, lang={}", movieId, language);
        return tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/movie/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", language)
                        .queryParam("append_to_response", "credits")
                        .build(movieId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .doOnNext(body -> log.error("TMDb movie API error {}: {}", response.statusCode(), body))
                        .map(body -> new TmdbClientException("Ошибка TMDb movie API: " + body)))
                .bodyToMono(TmdbMovieDetailsResponse.class)
                .block();
    }

    public TmdbPersonDetailsResponse getPersonDetails(long personId, String language) {
        log.debug("Запрашиваем детали актёра TMDb: id={}, lang={}", personId, language);
        return tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/person/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", language)
                        .queryParam("append_to_response", "combined_credits")
                        .build(personId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .doOnNext(body -> log.error("TMDb person API error {}: {}", response.statusCode(), body))
                        .map(body -> new TmdbClientException("Ошибка TMDb person API: " + body)))
                .bodyToMono(TmdbPersonDetailsResponse.class)
                .block();
    }
}
