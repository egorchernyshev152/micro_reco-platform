package com.example.recommender.repository;

import com.example.recommender.model.RecommendationRebuildLog;
import com.example.recommender.model.RecommendationRebuildStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface RecommendationRebuildLogRepository extends JpaRepository<RecommendationRebuildLog, Long> {

    Optional<RecommendationRebuildLog> findFirstByStatusInOrderByStartedAtDesc(Collection<RecommendationRebuildStatus> statuses);

    Optional<RecommendationRebuildLog> findFirstByOrderByStartedAtDesc();

    Optional<RecommendationRebuildLog> findFirstByStatusOrderByStartedAtDesc(RecommendationRebuildStatus status);
}
