package com.example.recommender.repository;

import com.example.recommender.model.RecommendationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationConfigRepository extends JpaRepository<RecommendationConfig, Long> {
}
