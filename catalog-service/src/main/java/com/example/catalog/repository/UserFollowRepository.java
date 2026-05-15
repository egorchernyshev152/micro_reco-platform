package com.example.catalog.repository;

import com.example.catalog.entity.UserFollow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Optional<UserFollow> findByFollower_IdAndTarget_Id(Long followerId, Long targetId);

    boolean existsByFollower_IdAndTarget_Id(Long followerId, Long targetId);

    long countByTarget_Id(Long targetId);

    long countByFollower_Id(Long followerId);

    List<UserFollow> findByFollower_IdOrderByCreatedAtDesc(Long followerId, Pageable pageable);

    List<UserFollow> findByTarget_IdOrderByCreatedAtDesc(Long targetId, Pageable pageable);
}
