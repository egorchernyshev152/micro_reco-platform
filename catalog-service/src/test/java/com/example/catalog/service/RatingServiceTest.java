package com.example.catalog.service;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.Rating;
import com.example.catalog.entity.User;
import com.example.catalog.entity.UserRole;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.integration.event.EventPublisher;
import com.example.catalog.repository.MovieRepository;
import com.example.catalog.repository.RatingRepository;
import com.example.catalog.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private RatingService ratingService;

    @Test
    void addShouldPersistRatingAndRefreshStats() {
        RatingDto request = RatingDto.builder()
                .userId(1L)
                .movieId(2L)
                .score(9)
                .build();
        when(ratingRepository.existsByUser_IdAndMovie_Id(1L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(movieRepository.findById(2L)).thenReturn(Optional.of(movie(2L)));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> {
            Rating rating = invocation.getArgument(0);
            rating.setId(10L);
            rating.setCreatedAt(Instant.EPOCH);
            return rating;
        });
        when(ratingRepository.calculateAverageScore(2L)).thenReturn(9.0);
        when(ratingRepository.countByMovie_Id(2L)).thenReturn(1L);

        RatingDto saved = ratingService.add(request);

        assertThat(saved.getId()).isEqualTo(10L);
        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository, atLeastOnce()).save(movieCaptor.capture());
        assertThat(movieCaptor.getValue().getAverageRating()).isEqualTo(9.0);
        assertThat(movieCaptor.getValue().getRatingsCount()).isEqualTo(1L);
    }

    @Test
    void addShouldThrowWhenDuplicateExists() {
        when(ratingRepository.existsByUser_IdAndMovie_Id(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> ratingService.add(RatingDto.builder()
                .userId(1L)
                .movieId(2L)
                .score(7)
                .build())).isInstanceOf(ConflictException.class);

        verifyNoInteractions(userRepository, movieRepository);
    }

    @Test
    void updateShouldReuseCreatedAtAndRefreshStats() {
        Rating existing = Rating.builder()
                .id(5L)
                .user(user(1L))
                .movie(movie(2L))
                .score(6)
                .createdAt(Instant.EPOCH)
                .build();
        when(ratingRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(ratingRepository.existsByUser_IdAndMovie_IdAndIdNot(1L, 2L, 5L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(movieRepository.findById(2L)).thenReturn(Optional.of(movie(2L)));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ratingRepository.calculateAverageScore(2L)).thenReturn(8.0);
        when(ratingRepository.countByMovie_Id(2L)).thenReturn(1L);

        RatingDto updated = ratingService.update(5L, RatingDto.builder()
                .userId(1L)
                .movieId(2L)
                .score(8)
                .build());

        assertThat(updated.getScore()).isEqualTo(8);
        assertThat(updated.getCreatedAt()).isEqualTo(Instant.EPOCH);
        verify(movieRepository, atLeastOnce()).save(any(Movie.class));
    }

    @Test
    void deleteShouldRemoveAndRefreshStats() {
        Rating rating = Rating.builder()
                .id(3L)
                .movie(movie(9L))
                .user(user(4L))
                .score(5)
                .createdAt(Instant.now())
                .build();
        when(ratingRepository.findById(3L)).thenReturn(Optional.of(rating));
        when(movieRepository.findById(9L)).thenReturn(Optional.of(movie(9L)));
        when(ratingRepository.calculateAverageScore(9L)).thenReturn(0.0);
        when(ratingRepository.countByMovie_Id(9L)).thenReturn(0L);

        ratingService.delete(3L);

        verify(ratingRepository).deleteById(3L);
        verify(movieRepository, atLeastOnce()).save(any(Movie.class));
    }

    @Test
    void getShouldThrowWhenRatingMissing() {
        when(ratingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ratingService.get(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void byMovieShouldDelegateToRepository() {
        when(ratingRepository.findByMovie_Id(2L)).thenReturn(List.of(
                Rating.builder().id(1L).movie(movie(2L)).user(user(3L)).score(7).createdAt(Instant.now()).build()
        ));

        List<RatingDto> dtos = ratingService.byMovie(2L);

        assertThat(dtos).hasSize(1);
        verify(ratingRepository).findByMovie_Id(2L);
    }

    @Test
    void setUserRatingShouldCreateWhenMissing() {
        when(ratingRepository.findByUser_IdAndMovie_Id(1L, 2L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(movieRepository.findById(2L)).thenReturn(Optional.of(movie(2L)));
        when(ratingRepository.save(any(Rating.class))).thenAnswer(invocation -> {
            Rating rating = invocation.getArgument(0);
            rating.setId(55L);
            return rating;
        });
        when(ratingRepository.calculateAverageScore(2L)).thenReturn(7.0);
        when(ratingRepository.countByMovie_Id(2L)).thenReturn(1L);

        RatingDto dto = ratingService.setUserRating(1L, 2L, 7);

        assertThat(dto.getId()).isEqualTo(55L);
        verify(ratingRepository).save(any(Rating.class));
        verify(eventPublisher).publishRatingEvent(1L, 2L, 7);
    }

    @Test
    void setUserRatingShouldUpdateExisting() {
        Rating existing = Rating.builder()
                .id(9L)
                .user(user(1L))
                .movie(movie(2L))
                .score(4)
                .build();
        when(ratingRepository.findByUser_IdAndMovie_Id(1L, 2L)).thenReturn(Optional.of(existing));
        when(ratingRepository.save(existing)).thenReturn(existing);
        when(ratingRepository.calculateAverageScore(2L)).thenReturn(6.0);
        when(ratingRepository.countByMovie_Id(2L)).thenReturn(1L);

        RatingDto dto = ratingService.setUserRating(1L, 2L, 9);

        assertThat(dto.getScore()).isEqualTo(9);
        verify(ratingRepository).save(existing);
        verify(eventPublisher).publishRatingEvent(1L, 2L, 9);
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .name("User " + id)
                .email("u" + id + "@mail.com")
                .role(UserRole.USER)
                .passwordHash("hash")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Movie movie(Long id) {
        return Movie.builder()
                .id(id)
                .title("Movie " + id)
                .genres(new java.util.LinkedHashSet<>())
                .countries(new java.util.LinkedHashSet<>())
                .tags(new java.util.LinkedHashSet<>())
                .build();
    }
}
