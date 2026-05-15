package com.example.catalog.repository;

import com.example.catalog.entity.Movie;
import com.example.catalog.entity.MovieStatus;
import com.example.catalog.repository.spec.MovieSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @BeforeEach
    void prepareData() {
        movieRepository.deleteAll();
        movieRepository.saveAll(List.of(
                movie("Hidden draft", MovieStatus.DRAFT, Set.of("Drama", "Indie")),
                movie("Space epic", MovieStatus.PUBLISHED, Set.of("Sci-Fi", "Adventure")),
                movie("Festival Highlight", MovieStatus.READY, Set.of("Drama", "Festival")),
                movie("Archive only", MovieStatus.ARCHIVED, Set.of("Documentary"))
        ));
    }

    @Test
    @DisplayName("MovieSpecifications поддерживают поиск по тексту, жанрам и статусам")
    void shouldFilterByQueryGenreAndStatus() {
        Specification<Movie> spec = Specification.where(MovieSpecifications.queryText("space"))
                .and(MovieSpecifications.withGenres(Set.of("Sci-Fi")))
                .and(MovieSpecifications.withStatuses(Set.of(MovieStatus.PUBLISHED)));

        List<Movie> result = movieRepository.findAll(spec);

        assertThat(result)
                .hasSize(1)
                .first()
                .extracting(Movie::getTitle)
                .isEqualTo("Space epic");
    }

    @Test
    @DisplayName("MovieSpecifications.withStatuses фильтрует несколько статусов")
    void shouldFilterMultipleStatuses() {
        Specification<Movie> spec = MovieSpecifications.withStatuses(Set.of(MovieStatus.DRAFT, MovieStatus.READY));

        List<Movie> result = movieRepository.findAll(spec);

        assertThat(result)
                .extracting(Movie::getStatus)
                .containsOnly(MovieStatus.DRAFT, MovieStatus.READY);
    }

    private Movie movie(String title, MovieStatus status, Set<String> genres) {
        return Movie.builder()
                .title(title)
                .status(status)
                .description("Test")
                .releaseDate(LocalDate.now())
                .releaseYear(LocalDate.now().getYear())
                .genres(new LinkedHashSet<>(genres))
                .countries(new LinkedHashSet<>(List.of("USA")))
                .tags(new LinkedHashSet<>(List.of("Test")))
                .build();
    }
}
