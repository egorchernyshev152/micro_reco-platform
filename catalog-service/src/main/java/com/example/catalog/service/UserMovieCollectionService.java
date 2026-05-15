package com.example.catalog.service;

import com.example.catalog.dto.MovieDto;
import com.example.catalog.dto.UserMovieCollectionSummaryDto;
import com.example.catalog.entity.UserMovieCollection;
import com.example.catalog.entity.UserMovieCollectionType;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.integration.event.EventPublisher;
import com.example.catalog.mapper.MovieMapper;
import com.example.catalog.repository.MovieRepository;
import com.example.catalog.repository.UserMovieCollectionRepository;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMovieCollectionService {

    private final UserMovieCollectionRepository collectionRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void add(Long userId, Long movieId, UserMovieCollectionType type) {
        ensureUserExists(userId);
        ensureMovieExists(movieId);
        if (collectionRepository.existsByUserIdAndMovieIdAndType(userId, movieId, type)) {
            return;
        }
        UserMovieCollection collection = UserMovieCollection.builder()
                .userId(userId)
                .movieId(movieId)
                .type(type)
                .build();
        collectionRepository.save(collection);
        publishCollectionEvent(userId, movieId, type);
    }

    @Transactional
    public void remove(Long userId, Long movieId, UserMovieCollectionType type) {
        collectionRepository.deleteByUserIdAndMovieIdAndType(userId, movieId, type);
    }

    @Transactional(readOnly = true)
    public List<MovieDto> getCollection(Long userId, UserMovieCollectionType type) {
        ensureUserExists(userId);
        List<UserMovieCollection> entries = collectionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type);
        if (entries.isEmpty()) {
            return List.of();
        }
        List<Long> movieIds = entries.stream().map(UserMovieCollection::getMovieId).toList();
        var order = new java.util.HashMap<Long, Integer>();
        for (int i = 0; i < movieIds.size(); i++) {
            order.put(movieIds.get(i), i);
        }
        return movieRepository.findByIdIn(movieIds).stream()
                .map(MovieMapper::toModel)
                .map(MovieMapper::toDto)
                .sorted(Comparator.comparingInt(movie -> order.getOrDefault(movie.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserMovieCollectionSummaryDto> getSummary(Long userId, List<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return List.of();
        }
        List<UserMovieCollection> entries = collectionRepository.findByUserIdAndMovieIdIn(userId, movieIds);
        Map<Long, List<UserMovieCollectionType>> grouped = entries.stream()
                .collect(Collectors.groupingBy(UserMovieCollection::getMovieId,
                        Collectors.mapping(UserMovieCollection::getType, Collectors.toList())));
        return movieIds.stream()
                .map(id -> UserMovieCollectionSummaryDto.builder()
                        .movieId(id)
                        .types(grouped.getOrDefault(id, List.of()))
                        .build())
                .toList();
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }
    }

    private void ensureMovieExists(Long movieId) {
        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException("Movie not found: " + movieId);
        }
    }

    private void publishCollectionEvent(Long userId, Long movieId, UserMovieCollectionType type) {
        String eventType = switch (type) {
            case FAVORITE -> "FAVORITE";
            case WATCHLIST -> "BOOKMARK";
            case WATCHED -> "FINISH_WATCHING";
        };
        eventPublisher.publishCollectionEvent(userId, movieId, eventType);
    }
}
