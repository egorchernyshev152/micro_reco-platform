package com.example.catalog.service.importer;

import com.example.catalog.config.TmdbProperties;
import com.example.catalog.dto.importer.MovieImportResponse;
import com.example.catalog.dto.importer.TmdbImportRequest;
import com.example.catalog.entity.Movie;
import com.example.catalog.integration.tmdb.TmdbClient;
import com.example.catalog.integration.tmdb.dto.TmdbDiscoverResponse;
import com.example.catalog.integration.tmdb.dto.TmdbGenreDto;
import com.example.catalog.integration.tmdb.dto.TmdbMovieDetailsResponse;
import com.example.catalog.integration.tmdb.dto.TmdbMovieSummary;
import com.example.catalog.integration.tmdb.dto.TmdbProductionCountryDto;
import com.example.catalog.integration.tmdb.dto.TmdbSpokenLanguageDto;
import com.example.catalog.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieImportServiceTest {

    @Mock
    private TmdbClient tmdbClient;
    @Mock
    private MovieRepository movieRepository;

    private MovieImportService movieImportService;

    @BeforeEach
    void setUp() {
        TmdbProperties properties = new TmdbProperties();
        properties.setApiKey("test-key");
        properties.setImageBaseUrl("https://image.tmdb.org/t/p");
        properties.setPosterSize("w500");
        properties.setBackdropSize("w780");

        movieImportService = new MovieImportService(tmdbClient, movieRepository, properties);
    }

    @Test
    void importPopularMoviesShouldPersistNewMovie() {
        TmdbImportRequest request = new TmdbImportRequest();
        request.setPages(1);

        TmdbMovieSummary summary = new TmdbMovieSummary();
        summary.setId(101L);
        summary.setTitle("Брат");
        summary.setOverview("Описание");
        summary.setReleaseDate("1997-05-17");

        TmdbDiscoverResponse discoverResponse = new TmdbDiscoverResponse();
        discoverResponse.setPage(1);
        discoverResponse.setTotalPages(1);
        discoverResponse.setResults(List.of(summary));

        TmdbMovieDetailsResponse details = movieDetails();

        when(tmdbClient.discoverMovies(eq(1), same(request), eq("US"))).thenReturn(discoverResponse);
        when(tmdbClient.getMovieDetails(101L, request.getLanguage())).thenReturn(details);
        when(movieRepository.findByTmdbId(101L)).thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        MovieImportResponse response = movieImportService.importPopularMovies(request);

        assertThat(response.getImportedMovies()).isEqualTo(1);
        assertThat(response.getUpdatedMovies()).isZero();

        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie saved = movieCaptor.getValue();
        assertThat(saved.getTmdbId()).isEqualTo(101L);
        assertThat(saved.getTitle()).isEqualTo("Брат");
        assertThat(saved.getReleaseYear()).isEqualTo(1997);
        assertThat(saved.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
        assertThat(saved.getGenres()).containsExactly("Драма");
        assertThat(saved.getCountries()).containsExactly("Россия");
    }

    @Test
    void importPopularMoviesShouldUpdateExistingMovie() {
        TmdbImportRequest request = new TmdbImportRequest();
        request.setPages(1);

        TmdbMovieSummary summary = new TmdbMovieSummary();
        summary.setId(202L);
        summary.setTitle("Кинолента");

        TmdbDiscoverResponse discoverResponse = new TmdbDiscoverResponse();
        discoverResponse.setPage(1);
        discoverResponse.setTotalPages(1);
        discoverResponse.setResults(List.of(summary));

        TmdbMovieDetailsResponse details = movieDetails();
        details.setId(202L);
        details.setTitle("Кинолента обновленная");

        Movie existing = Movie.builder()
                .id(5L)
                .tmdbId(202L)
                .title("Old")
                .genres(new LinkedHashSet<>())
                .countries(new LinkedHashSet<>())
                .tags(new LinkedHashSet<>())
                .build();

        when(tmdbClient.discoverMovies(eq(1), same(request), eq("US"))).thenReturn(discoverResponse);
        when(tmdbClient.getMovieDetails(202L, request.getLanguage())).thenReturn(details);
        when(movieRepository.findByTmdbId(202L)).thenReturn(Optional.of(existing));
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        MovieImportResponse response = movieImportService.importPopularMovies(request);

        assertThat(response.getImportedMovies()).isZero();
        assertThat(response.getUpdatedMovies()).isEqualTo(1);
    }

    @Test
    void importPopularMoviesShouldIterateOverMultipleOriginCountries() {
        TmdbImportRequest request = new TmdbImportRequest();
        request.setPages(1);
        request.setOriginCountries(new LinkedHashSet<>(List.of("ru", "gb")));

        TmdbMovieSummary summary = new TmdbMovieSummary();
        summary.setId(303L);
        summary.setTitle("Test");

        TmdbDiscoverResponse discoverResponse = new TmdbDiscoverResponse();
        discoverResponse.setPage(1);
        discoverResponse.setTotalPages(1);
        discoverResponse.setResults(List.of(summary));

        TmdbMovieDetailsResponse details = movieDetails();
        details.setId(303L);

        when(tmdbClient.discoverMovies(eq(1), same(request), eq("RU"))).thenReturn(discoverResponse);
        when(tmdbClient.discoverMovies(eq(1), same(request), eq("GB"))).thenReturn(discoverResponse);
        when(tmdbClient.getMovieDetails(303L, request.getLanguage())).thenReturn(details);
        when(movieRepository.findByTmdbId(303L)).thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class))).thenAnswer(inv -> inv.getArgument(0));

        MovieImportResponse response = movieImportService.importPopularMovies(request);

        assertThat(response.getImportedMovies()).isEqualTo(2);
        verify(tmdbClient, times(1)).discoverMovies(1, request, "RU");
        verify(tmdbClient, times(1)).discoverMovies(1, request, "GB");
    }

    private TmdbMovieDetailsResponse movieDetails() {
        TmdbMovieDetailsResponse details = new TmdbMovieDetailsResponse();
        details.setId(101L);
        details.setTitle("Брат");
        details.setOverview("Описание фильма");
        details.setReleaseDate("1997-05-17");
        details.setRuntime(100);
        details.setPosterPath("/poster.jpg");
        details.setBackdropPath("/backdrop.jpg");

        TmdbGenreDto genre = new TmdbGenreDto();
        genre.setId(1L);
        genre.setName("Драма");
        details.setGenres(List.of(genre));

        TmdbProductionCountryDto country = new TmdbProductionCountryDto();
        country.setIsoCode("RU");
        country.setName("Россия");
        details.setProductionCountries(List.of(country));

        TmdbSpokenLanguageDto lang = new TmdbSpokenLanguageDto();
        lang.setIsoCode("ru");
        lang.setName("Русский");
        details.setSpokenLanguages(List.of(lang));

        return details;
    }
}
