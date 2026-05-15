package com.example.catalog.service;

import com.example.catalog.config.TmdbProperties;
import com.example.catalog.dto.ActorDetailsDto;
import com.example.catalog.dto.ActorKnownForDto;
import com.example.catalog.integration.tmdb.TmdbClient;
import com.example.catalog.integration.tmdb.dto.TmdbCreditDto;
import com.example.catalog.integration.tmdb.dto.TmdbPersonDetailsResponse;
import com.example.catalog.entity.Movie;
import com.example.catalog.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActorService {

    private static final String DEFAULT_LANGUAGE = "ru-RU";

    private final TmdbClient tmdbClient;
    private final TmdbProperties tmdbProperties;
    private final MovieRepository movieRepository;

    public ActorDetailsDto getActorDetails(long tmdbId, String language) {
        String resolvedLanguage = StringUtils.hasText(language) ? language : DEFAULT_LANGUAGE;
        TmdbPersonDetailsResponse response = tmdbClient.getPersonDetails(tmdbId, resolvedLanguage);
        if (response == null) {
            return null;
        }
        return ActorDetailsDto.builder()
                .tmdbId(response.getId())
                .name(response.getName())
                .biography(response.getBiography())
                .birthday(response.getBirthday())
                .deathday(response.getDeathday())
                .placeOfBirth(response.getPlaceOfBirth())
                .profileUrl(tmdbProperties.buildProfileUrl(response.getProfilePath()))
                .knownForDepartment(response.getKnownForDepartment())
                .popularity(response.getPopularity())
                .alsoKnownAs(response.getAlsoKnownAs())
                .highlights(buildHighlights(response))
                .knownFor(buildKnownFor(response))
                .build();
    }

    private List<ActorKnownForDto> buildKnownFor(TmdbPersonDetailsResponse response) {
        if (response.getCombinedCredits() == null || CollectionUtils.isEmpty(response.getCombinedCredits().getCast())) {
            return List.of();
        }
        return response.getCombinedCredits().getCast().stream()
                .filter(credit -> StringUtils.hasText(resolveCreditTitle(credit)))
                .sorted(Comparator
                        .comparing(TmdbCreditDto::getVoteCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(TmdbCreditDto::getPopularity, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(credit -> {
                    Movie matchedMovie = movieRepository.findByTmdbId(credit.getId()).orElse(null);
                    return ActorKnownForDto.builder()
                        .title(resolveCreditTitle(credit))
                        .year(resolveCreditYearValue(credit))
                        .voteAverage(credit.getVoteAverage() == null ? null : Math.round(credit.getVoteAverage() * 10.0) / 10.0)
                        .tmdbId(credit.getId())
                        .movieId(matchedMovie != null ? matchedMovie.getId() : null)
                        .catalogRating(matchedMovie != null ? matchedMovie.getAverageRating() : null)
                        .mediaType(credit.getMediaType())
                        .character(credit.getCharacter())
                        .build();
                })
                .collect(Collectors.toList());
    }

    private List<String> buildHighlights(TmdbPersonDetailsResponse response) {
        List<String> builder = new ArrayList<>();
        if (StringUtils.hasText(response.getKnownForDepartment())) {
            builder.add("Направление: " + response.getKnownForDepartment());
        }
        if (response.getPopularity() != null) {
            builder.add("Популярность TMDb: " + String.format(Locale.US, "%.1f", response.getPopularity()));
        }
        if (response.getCombinedCredits() != null && !CollectionUtils.isEmpty(response.getCombinedCredits().getCast())) {
            builder.add("Работ в базе TMDb: " + response.getCombinedCredits().getCast().size());
        }
        if (StringUtils.hasText(response.getDeathday())) {
            builder.add("Последние данные: " + formatDate(response.getDeathday()));
        }
        return builder;
    }

    private String resolveCreditTitle(TmdbCreditDto credit) {
        if (StringUtils.hasText(credit.getTitle())) {
            return credit.getTitle();
        }
        return credit.getName();
    }

    private Integer resolveCreditYearValue(TmdbCreditDto credit) {
        String source = StringUtils.hasText(credit.getReleaseDate()) ? credit.getReleaseDate() : credit.getFirstAirDate();
        if (!StringUtils.hasText(source)) {
            return null;
        }
        try {
            return LocalDate.parse(source).getYear();
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String formatDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(raw);
            return date.toString();
        } catch (DateTimeParseException ex) {
            return raw;
        }
    }
}
