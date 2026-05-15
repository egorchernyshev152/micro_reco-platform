package com.example.recommender.repository;

import com.example.recommender.model.UserRecommendationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRecommendationPreferenceRepository extends JpaRepository<UserRecommendationPreference, Long> {
}
