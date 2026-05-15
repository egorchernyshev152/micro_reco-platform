package com.example.catalog.service;

import com.example.catalog.dto.MovieDto;
import com.example.catalog.dto.MoviePageResponse;
import com.example.catalog.dto.MovieSearchRequest;
import com.example.catalog.entity.Movie;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    @Test
    void createShouldPersistMovieAndReturnDto() {
        MovieDto request = MovieDto.builder()
                .title("Inception")
                .description("Dream within a dream")
                .build();
        Movie saved = movieEntity(42L, "Inception", "Dream within a dream");

        when(movieRepository.save(any(Movie.class))).thenReturn(saved);

        MovieDto result = movieService.create(request);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getTitle()).isEqualTo("Inception");

        ArgumentCaptor<Movie> captor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getTitle()).isEqualTo("Inception");
    }

    @Test
    void updateShouldModifyExistingMovie() {
        Movie existing = movieEntity(7L, "Old title", "Old desc");
        MovieDto update = MovieDto.builder()
                .title("New title")
                .description("Fresh desc")
                .genres(Set.of("sci-fi"))
                .countries(Set.of("USA"))
                .build();

        when(movieRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(movieRepository.save(existing)).thenAnswer(invocation -> invocation.getArgument(0));

        MovieDto result = movieService.update(7L, update);

        assertThat(result.getTitle()).isEqualTo("New title");
        verify(movieRepository).save(existing);
    }

    @Test
    void updateShouldThrowWhenMovieMissing() {
        when(movieRepository.findById(555L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.update(555L, MovieDto.builder().title("t").build()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getShouldReturnMovie() {
        when(movieRepository.findById(9L)).thenReturn(Optional.of(movieEntity(9L, "Matrix", "Desc")));

        MovieDto dto = movieService.get(9L);

        assertThat(dto.getId()).isEqualTo(9L);
        assertThat(dto.getTitle()).isEqualTo("Matrix");
    }

    @Test
    void getShouldThrowWhenMovieMissing() {
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.get(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findByIdsShouldMapResults() {
        when(movieRepository.findByIdIn(Set.of(1L, 2L))).thenReturn(List.of(
                movieEntity(1L, "A", "Desc"),
                movieEntity(2L, "B", "Desc")
        ));

        List<MovieDto> dtos = movieService.findByIds(Set.of(1L, 2L));

        assertThat(dtos).extracting(MovieDto::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void searchShouldCallRepositoryWithSpecificationAndPageable() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(movieRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(movieEntity(3L, "Blade Runner", "Neo noir")), pageable, 1));
        MovieSearchRequest request = new MovieSearchRequest();
        request.setGenres(Set.of("sci-fi"));
        request.setLimit(5);

        MoviePageResponse page = movieService.search(request);

        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(1);
        verify(movieRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    private Movie movieEntity(Long id, String title, String description) {
        return Movie.builder()
                .id(id)
                .title(title)
                .description(description)
                .genres(new java.util.LinkedHashSet<>())
                .countries(new java.util.LinkedHashSet<>())
                .tags(new java.util.LinkedHashSet<>())
                .build();
    }
}
