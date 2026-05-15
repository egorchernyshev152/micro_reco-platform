package com.example.recommender.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recommender_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationConfig {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "training_period", nullable = false, length = 32)
    private String trainingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_algorithm", nullable = false, length = 40)
    private AlgorithmType defaultAlgorithm;

    @Column(name = "default_strategy_id")
    private Long defaultStrategyId;

    @Column(name = "recommendation_limit", nullable = false)
    private Integer recommendationLimit;

    @Column(name = "rebuild_batch_size", nullable = false)
    private Integer rebuildBatchSize;

    @Column(name = "max_users_per_job", nullable = false)
    private Integer maxUsersPerJob;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (id == null) {
            id = SINGLETON_ID;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
