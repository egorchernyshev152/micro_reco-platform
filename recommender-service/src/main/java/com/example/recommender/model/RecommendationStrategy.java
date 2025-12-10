package com.example.recommender.model;

import com.example.recommender.util.MapToJsonConverter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "recommendation_strategies")
public class RecommendationStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private AlgorithmType algorithm;

    @Convert(converter = MapToJsonConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> eventWeights;

    @Column(name = "time_decay_half_life_days")
    private Integer timeDecayHalfLifeDays;

    @Column(name = "min_events_per_user")
    private Integer minEventsPerUser;

    @Column(name = "candidate_limit")
    private Integer candidateLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "fallback_algorithm")
    private AlgorithmType fallbackAlgorithm;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
