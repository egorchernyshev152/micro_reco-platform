package com.example.catalog.service;

import com.example.catalog.dto.ActorFavoriteDto;
import com.example.catalog.dto.ActorFavoriteRequest;
import com.example.catalog.entity.UserActorFavorite;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.integration.event.EventPublisher;
import com.example.catalog.repository.UserActorFavoriteRepository;
import com.example.catalog.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserActorFavoriteService {

    private final UserActorFavoriteRepository repository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public ActorFavoriteDto add(Long userId, Long actorTmdbId, ActorFavoriteRequest request) {
        ensureUserExists(userId);
        if (repository.existsByUserIdAndActorTmdbId(userId, actorTmdbId)) {
            return toDto(repository.findByUserIdAndActorTmdbId(userId, actorTmdbId).orElseThrow());
        }
        UserActorFavorite favorite = UserActorFavorite.builder()
                .userId(userId)
                .actorTmdbId(actorTmdbId)
                .actorName(request.getActorName())
                .profileUrl(request.getProfileUrl())
                .build();
        UserActorFavorite saved = repository.save(favorite);
        eventPublisher.publishActorFavoriteEvent(userId, actorTmdbId, request.getActorName());
        return toDto(saved);
    }

    @Transactional
    public void remove(Long userId, Long actorTmdbId) {
        repository.deleteByUserIdAndActorTmdbId(userId, actorTmdbId);
    }

    @Transactional(readOnly = true)
    public List<ActorFavoriteDto> list(Long userId) {
        ensureUserExists(userId);
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found: " + userId);
        }
    }

    private ActorFavoriteDto toDto(UserActorFavorite favorite) {
        return ActorFavoriteDto.builder()
                .actorTmdbId(favorite.getActorTmdbId())
                .actorName(favorite.getActorName())
                .profileUrl(favorite.getProfileUrl())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
