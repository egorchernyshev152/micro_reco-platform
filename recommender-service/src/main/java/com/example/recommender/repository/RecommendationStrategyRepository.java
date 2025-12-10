package com.example.recommender.repository;

import com.example.recommender.model.RecommendationStrategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationStrategyRepository extends JpaRepository<RecommendationStrategy, Long> {
    List<RecommendationStrategy> findByActiveTrue();
}
