package com.example.catalog.service;

import com.example.catalog.dto.RatingDto;
import com.example.catalog.entity.Movie;
import com.example.catalog.entity.Rating;
import com.example.catalog.entity.User;
import com.example.catalog.exception.ConflictException;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.mapper.RatingMapper;
import com.example.catalog.model.RatingModel;
import com.example.catalog.repository.MovieRepository;
import com.example.catalog.repository.RatingRepository;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RatingService {
    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public RatingDto add(RatingDto dto) {
        ensureUnique(dto.getUserId(), dto.getMovieId(), null);
        RatingModel model = RatingMapper.fromDto(dto);
        Rating saved = ratingRepository.save(toEntityWithRelations(model));
        refreshMovieStats(model.getMovieId());
        return RatingMapper.toDto(RatingMapper.toModel(saved));
    }

    @Transactional
    public RatingDto update(Long id, RatingDto dto) {
        Rating existing = ratingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rating not found: " + id));
        ensureUnique(dto.getUserId(), dto.getMovieId(), id);
        RatingModel fromDto = RatingMapper.fromDto(dto);
        RatingModel model = RatingModel.builder()
                .id(id)
                .userId(fromDto.getUserId())
                .movieId(fromDto.getMovieId())
                .score(fromDto.getScore())
                .createdAt(existing.getCreatedAt())
                .build();
        Rating saved = ratingRepository.save(toEntityWithRelations(model));
        refreshMovieStats(model.getMovieId());
        return RatingMapper.toDto(RatingMapper.toModel(saved));
    }

    public List<RatingDto> byUser(Long userId) {
        return ratingRepository.findByUser_Id(userId).stream().map(RatingMapper::toModel).map(RatingMapper::toDto).toList();
    }

    public List<RatingDto> byMovie(Long movieId) {
        return ratingRepository.findByMovie_Id(movieId).stream().map(RatingMapper::toModel).map(RatingMapper::toDto).toList();
    }

    public RatingDto get(Long id) {
        return ratingRepository.findById(id)
                .map(RatingMapper::toModel)
                .map(RatingMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Rating not found: " + id));
    }

    public List<RatingDto> getAll() {
        return ratingRepository.findAll().stream().map(RatingMapper::toModel).map(RatingMapper::toDto).toList();
    }

    @Transactional
    public void delete(Long id) {
        Rating rating = ratingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rating not found: " + id));
        Long movieId = rating.getMovie().getId();
        ratingRepository.deleteById(id);
        refreshMovieStats(movieId);
    }

    @Transactional
    public RatingDto setUserRating(Long userId, Long movieId, int score) {
        Rating rating = ratingRepository.findByUser_IdAndMovie_Id(userId, movieId)
                .orElseGet(() -> Rating.builder()
                        .user(userRepository.findById(userId)
                                .orElseThrow(() -> new NotFoundException("User not found: " + userId)))
                        .movie(movieRepository.findById(movieId)
                                .orElseThrow(() -> new NotFoundException("Movie not found: " + movieId)))
                        .build());
        rating.setScore(score);
        Rating saved = ratingRepository.save(rating);
        refreshMovieStats(movieId);
        return RatingMapper.toDto(RatingMapper.toModel(saved));
    }

    @Transactional(readOnly = true)
    public RatingDto getUserRating(Long userId, Long movieId) {
        return ratingRepository.findByUser_IdAndMovie_Id(userId, movieId)
                .map(RatingMapper::toModel)
                .map(RatingMapper::toDto)
                .orElse(null);
    }

    @Transactional
    public void deleteUserRating(Long userId, Long movieId) {
        Rating rating = ratingRepository.findByUser_IdAndMovie_Id(userId, movieId)
                .orElseThrow(() -> new NotFoundException("Rating not found for user: " + userId));
        ratingRepository.delete(rating);
        refreshMovieStats(movieId);
    }

    private Rating toEntityWithRelations(RatingModel model) {
        User user = userRepository.findById(model.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found: " + model.getUserId()));
        Movie movie = movieRepository.findById(model.getMovieId())
                .orElseThrow(() -> new NotFoundException("Movie not found: " + model.getMovieId()));
        return RatingMapper.toEntity(model, user, movie);
    }

    private void ensureUnique(Long userId, Long movieId, Long currentId) {
        if (userId == null || movieId == null) return;
        boolean conflict = currentId == null
                ? ratingRepository.existsByUser_IdAndMovie_Id(userId, movieId)
                : ratingRepository.existsByUser_IdAndMovie_IdAndIdNot(userId, movieId, currentId);
        if (conflict) {
            throw new ConflictException("Rating already exists for user " + userId + " and movie " + movieId);
        }
    }

    private void refreshMovieStats(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found: " + movieId));
        Double avg = ratingRepository.calculateAverageScore(movieId);
        long count = ratingRepository.countByMovie_Id(movieId);
        movie.setAverageRating(avg == null ? null : Math.round(avg * 100.0) / 100.0);
        movie.setRatingsCount(count);
        movieRepository.save(movie);
    }
}
