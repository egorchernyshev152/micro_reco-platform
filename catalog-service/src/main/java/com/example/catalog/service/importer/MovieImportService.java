package com.example.catalog.service.importer;

import com.example.catalog.config.TmdbProperties;
import com.example.catalog.dto.importer.MovieImportResponse;
import com.example.catalog.dto.importer.TmdbImportRequest;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.MovieCastMember;
import com.example.catalog.entity.MovieStatus;
import com.example.catalog.integration.tmdb.TmdbClient;
import com.example.catalog.integration.tmdb.dto.TmdbDiscoverResponse;
import com.example.catalog.integration.tmdb.dto.TmdbCastDto;
import com.example.catalog.integration.tmdb.dto.TmdbGenreDto;
import com.example.catalog.integration.tmdb.dto.TmdbMovieDetailsResponse;
import com.example.catalog.integration.tmdb.dto.TmdbMovieSummary;
import com.example.catalog.integration.tmdb.dto.TmdbProductionCountryDto;
import com.example.catalog.integration.tmdb.dto.TmdbSpokenLanguageDto;
import com.example.catalog.repository.MovieRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieImportService {

    private final TmdbClient tmdbClient;
    private final MovieRepository movieRepository;
    private final TmdbProperties tmdbProperties;

    @Transactional
    public MovieImportResponse importPopularMovies(TmdbImportRequest request) {
        AtomicInteger processedPages = new AtomicInteger();
        AtomicInteger imported = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();

        for (String originCountry : request.resolveOriginCountries()) {
            log.info("Начинаем импорт TMDb для страны {}", originCountry);
            for (int page = 1; page <= request.getPages(); page++) {
                TmdbDiscoverResponse discoverResponse = tmdbClient.discoverMovies(page, request, originCountry);
                if (discoverResponse == null || CollectionUtils.isEmpty(discoverResponse.getResults())) {
                    log.info("TMDb вернул пустой список на странице {} (страна {}), остановка импорта", page, originCountry);
                    break;
                }
                processedPages.incrementAndGet();
                discoverResponse.getResults().forEach(summary -> importSingleMovie(summary, request, imported, updated, skipped));
                if (discoverResponse.getPage() >= discoverResponse.getTotalPages()) {
                    log.info("Достигли последней доступной страницы TMDb ({}) для страны {}", discoverResponse.getTotalPages(), originCountry);
                    break;
                }
            }
        }

        return MovieImportResponse.builder()
                .requestedPages(request.getPages())
                .processedPages(processedPages.get())
                .importedMovies(imported.get())
                .updatedMovies(updated.get())
                .skippedMovies(skipped.get())
                .build();
    }

    private void importSingleMovie(TmdbMovieSummary summary,
                                   TmdbImportRequest request,
                                   AtomicInteger imported,
                                   AtomicInteger updated,
                                   AtomicInteger skipped) {
        try {
            TmdbMovieDetailsResponse details = tmdbClient.getMovieDetails(summary.getId(), request.getLanguage());
            if (details == null) {
                log.warn("TMDb вернул пустые детали для фильма {}", summary.getId());
                skipped.incrementAndGet();
                return;
            }
            if (!StringUtils.hasText(details.getTitle())) {
                log.debug("Пропускаем фильм {} — нет локализованного названия", summary.getId());
                skipped.incrementAndGet();
                return;
            }
            boolean created = upsertMovie(details, summary);
            if (created) {
                imported.incrementAndGet();
            } else {
                updated.incrementAndGet();
            }
        } catch (Exception e) {
            skipped.incrementAndGet();
            log.warn("Не удалось импортировать фильм {}: {}", summary.getId(), e.getMessage(), e);
        }
    }

    private boolean upsertMovie(TmdbMovieDetailsResponse details, TmdbMovieSummary summary) {
        Movie movie = movieRepository.findByTmdbId(details.getId()).orElseGet(Movie::new);
        boolean isNew = movie.getId() == null;

        movie.setTmdbId(details.getId());
        movie.setTitle(firstNonBlank(details.getTitle(), summary.getTitle(), summary.getOriginalTitle()));
        movie.setOriginalTitle(details.getOriginalTitle());
        movie.setOriginalLanguage(details.getOriginalLanguage());
        movie.setDescription(buildDescription(summary, details));
        movie.setSynopsis(buildSynopsis(details, summary));
        movie.setTagline(details.getTagline());
        movie.setStatus(resolveStatus(details.getStatus()));
        movie.setDurationMinutes(details.getRuntime());
        movie.setBudget(details.getBudget());
        movie.setRevenue(details.getRevenue());

        LocalDate releaseDate = parseReleaseDate(details.getReleaseDate());
        movie.setReleaseDate(releaseDate);
        movie.setReleaseYear(releaseDate != null ? releaseDate.getYear() : null);

        movie.setPosterUrl(tmdbProperties.buildPosterUrl(details.getPosterPath()));
        movie.setBackdropUrl(tmdbProperties.buildBackdropUrl(details.getBackdropPath()));

        movie.setGenres(replaceValues(movie.getGenres(), details.getGenres().stream()
                .map(TmdbGenreDto::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new))));

        movie.setCountries(replaceValues(movie.getCountries(), details.getProductionCountries().stream()
                .map(TmdbProductionCountryDto::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new))));

        movie.setTags(replaceValues(movie.getTags(), buildTags(details)));
        movie.setCast(buildCast(details));
        movie.setImportedRating(details.getVoteAverage());
        if (movie.getAverageRating() == null) {
            movie.setAverageRating(details.getVoteAverage());
        }
        movie.setRatingsCount(details.getVoteCount() == null ? null : details.getVoteCount().longValue());

        movieRepository.save(movie);
        return isNew;
    }

    private Set<String> buildTags(TmdbMovieDetailsResponse details) {
        Set<String> tags = new LinkedHashSet<>();
        details.getGenres().stream()
                .map(TmdbGenreDto::getName)
                .filter(StringUtils::hasText)
                .forEach(tags::add);
        details.getSpokenLanguages().stream()
                .map(TmdbSpokenLanguageDto::getName)
                .filter(StringUtils::hasText)
                .forEach(tags::add);
        details.getProductionCountries().stream()
                .map(TmdbProductionCountryDto::getIsoCode)
                .filter(StringUtils::hasText)
                .forEach(tags::add);
        return tags;
    }

    private Set<String> replaceValues(Set<String> current, Set<String> newValues) {
        Set<String> target = current != null ? current : new LinkedHashSet<>();
        target.clear();
        if (!CollectionUtils.isEmpty(newValues)) {
            target.addAll(newValues);
        }
        return target;
    }

    private LocalDate parseReleaseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            log.debug("Не удалось распарсить дату релиза {}: {}", raw, ex.getMessage());
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String buildDescription(TmdbMovieSummary summary, TmdbMovieDetailsResponse details) {
        String summaryText = summary != null ? summary.getOverview() : null;
        if (StringUtils.hasText(summaryText)) {
            return summaryText;
        }
        if (details != null && StringUtils.hasText(details.getTagline())) {
            return details.getTagline();
        }
        if (details != null && StringUtils.hasText(details.getOverview())) {
            return details.getOverview();
        }
        return null;
    }

    private String buildSynopsis(TmdbMovieDetailsResponse details, TmdbMovieSummary summary) {
        StringBuilder builder = new StringBuilder();
        if (details != null && StringUtils.hasText(details.getOverview())) {
            builder.append(details.getOverview().trim());
        } else if (summary != null && StringUtils.hasText(summary.getOverview())) {
            builder.append(summary.getOverview().trim());
        }
        if (details != null && StringUtils.hasText(details.getTagline())) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(details.getTagline().trim());
        }
        if (details != null && details.getRuntime() != null) {
            builder.append("\n\nДлительность: ").append(details.getRuntime()).append(" мин.");
        }
        if (details != null && !CollectionUtils.isEmpty(details.getGenres())) {
            String genres = details.getGenres().stream()
                    .map(TmdbGenreDto::getName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
            if (StringUtils.hasText(genres)) {
                builder.append("\nЖанры: ").append(genres);
            }
        }
        if (details != null && !CollectionUtils.isEmpty(details.getProductionCountries())) {
            String countries = details.getProductionCountries().stream()
                    .map(TmdbProductionCountryDto::getName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
            if (StringUtils.hasText(countries)) {
                builder.append("\nСтраны: ").append(countries);
            }
        }
        if (details != null && details.getCredits() != null && !CollectionUtils.isEmpty(details.getCredits().getCast())) {
            String cast = details.getCredits().getCast().stream()
                    .sorted(Comparator.comparingInt(c -> c.getOrder() != null ? c.getOrder() : Integer.MAX_VALUE))
                    .limit(5)
                    .map(TmdbCastDto::getName)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
            if (StringUtils.hasText(cast)) {
                builder.append("\nВ ролях: ").append(cast);
            }
        }
        String result = builder.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private List<MovieCastMember> buildCast(TmdbMovieDetailsResponse details) {
        if (details.getCredits() == null || CollectionUtils.isEmpty(details.getCredits().getCast())) {
            return List.of();
        }
        return details.getCredits().getCast().stream()
                .sorted(Comparator.comparingInt(c -> c.getOrder() != null ? c.getOrder() : Integer.MAX_VALUE))
                .limit(15)
                .map(this::toCastMember)
                .collect(Collectors.toList());
    }

    private MovieStatus resolveStatus(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return MovieStatus.DRAFT;
        }
        return switch (rawStatus.trim().toLowerCase()) {
            case "released" -> MovieStatus.PUBLISHED;
            case "in production", "post production" -> MovieStatus.READY;
            case "canceled" -> MovieStatus.ARCHIVED;
            default -> MovieStatus.DRAFT;
        };
    }

    private MovieCastMember toCastMember(TmdbCastDto dto) {
        return MovieCastMember.builder()
                .personTmdbId(dto.getId())
                .name(dto.getName())
                .character(dto.getCharacter())
                .profileUrl(tmdbProperties.buildProfileUrl(dto.getProfilePath()))
                .orderIndex(dto.getOrder())
                .build();
    }
}
