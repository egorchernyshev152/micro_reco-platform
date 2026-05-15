package com.example.catalog.repository;

import com.example.catalog.entity.UserActorFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserActorFavoriteRepository extends JpaRepository<UserActorFavorite, Long> {
    boolean existsByUserIdAndActorTmdbId(Long userId, Long actorTmdbId);

    void deleteByUserIdAndActorTmdbId(Long userId, Long actorTmdbId);

    Optional<UserActorFavorite> findByUserIdAndActorTmdbId(Long userId, Long actorTmdbId);

    List<UserActorFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
