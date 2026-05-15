package com.example.catalog.service;

import com.example.catalog.dto.PageResponse;
import com.example.catalog.dto.ReviewDto;
import com.example.catalog.dto.ReviewRequest;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.Review;
import com.example.catalog.entity.ReviewStatus;
import com.example.catalog.entity.User;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.mapper.ReviewMapper;
import com.example.catalog.repository.MovieRepository;
import com.example.catalog.repository.ReviewRepository;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewDto submitReview(Long userId, Long movieId, ReviewRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User is required");
        }
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found: " + movieId));
        String content = request.getContent() != null ? request.getContent().trim() : "";
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Текст отзыва не может быть пустым");
        }
        Review review = reviewRepository.findByMovie_IdAndAuthor_Id(movieId, userId)
                .orElseGet(() -> Review.builder()
                        .author(author)
                        .movie(movie)
                        .build());
        review.setAuthor(author);
        review.setMovie(movie);
        review.setScore(request.getScore());
        review.setContent(content);
        review.setStatus(ReviewStatus.PENDING);
        review.setFlagged(false);
        review.setFlaggedAt(null);
        review.setLastModerationReason(null);
        review.setModeratedAt(null);
        review.setModeratedBy(null);
        Review saved = reviewRepository.save(review);
        return ReviewMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public ReviewDto getUserReview(Long userId, Long movieId) {
        if (userId == null) {
            throw new IllegalArgumentException("User is required");
        }
        return reviewRepository.findByMovie_IdAndAuthor_Id(movieId, userId)
                .map(ReviewMapper::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> getPublishedReviews(Long movieId, int page, int size) {
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Movie not found: " + movieId);
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> result = reviewRepository.findByMovie_IdAndStatus(movieId, ReviewStatus.PUBLISHED, pageable);
        return PageResponse.<ReviewDto>builder()
                .items(result.getContent().stream().map(ReviewMapper::toDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .hasNext(result.hasNext())
                .build();
    }
}
