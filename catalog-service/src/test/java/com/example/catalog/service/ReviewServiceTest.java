package com.example.catalog.service;

import com.example.catalog.dto.PageResponse;
import com.example.catalog.dto.ReviewDto;
import com.example.catalog.dto.ReviewRequest;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.Review;
import com.example.catalog.entity.ReviewStatus;
import com.example.catalog.entity.User;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.MovieRepository;
import com.example.catalog.repository.ReviewRepository;
import com.example.catalog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Movie movie;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Alice").build();
        movie = Movie.builder().id(2L).title("Movie").build();
    }

    @Test
    void submitReviewCreatesPendingReview() {
        ReviewRequest request = new ReviewRequest();
        request.setScore(8);
        request.setContent("  Отличный фильм, обязательно к просмотру!  ");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(movieRepository.findById(2L)).thenReturn(Optional.of(movie));
        when(reviewRepository.findByMovie_IdAndAuthor_Id(2L, 1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review entity = invocation.getArgument(0);
            entity.setId(5L);
            entity.setCreatedAt(Instant.now());
            return entity;
        });

        ReviewDto dto = reviewService.submitReview(1L, 2L, request);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getScore()).isEqualTo(8);
        assertThat(dto.getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(dto.getContent()).isEqualTo("Отличный фильм, обязательно к просмотру!");
    }

    @Test
    void submitReviewThrowsIfMovieMissing() {
        ReviewRequest request = new ReviewRequest();
        request.setScore(7);
        request.setContent("Неплохая драма с хорошей игрой актеров.");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(movieRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> reviewService.submitReview(1L, 9L, request));
    }

    @Test
    void getPublishedReviewsReturnsPage() {
        Review review = Review.builder()
                .id(11L)
                .movie(movie)
                .author(user)
                .score(9)
                .content("text")
                .status(ReviewStatus.PUBLISHED)
                .build();
        when(movieRepository.existsById(2L)).thenReturn(true);
        when(reviewRepository.findByMovie_IdAndStatus(eq(2L), eq(ReviewStatus.PUBLISHED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 6), 1));

        PageResponse<ReviewDto> page = reviewService.getPublishedReviews(2L, 0, 6);

        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getId()).isEqualTo(11L);
    }

    @Test
    void getPublishedReviewsFailsIfMovieMissing() {
        when(movieRepository.existsById(777L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> reviewService.getPublishedReviews(777L, 0, 5));
    }
}
