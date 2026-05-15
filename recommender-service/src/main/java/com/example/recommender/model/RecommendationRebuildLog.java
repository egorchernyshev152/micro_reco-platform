package com.example.recommender.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recommendation_rebuild_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationRebuildLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RecommendationRebuildStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "processed_users")
    private Integer processedUsers;

    @Column(name = "total_users")
    private Integer totalUsers;

    @Column(name = "initiator", length = 255)
    private String initiator;

    @Column(name = "training_period", length = 32)
    private String trainingPeriod;

    @Lob
    private String message;

    @PrePersist
    public void onCreate() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
        if (processedUsers == null) {
            processedUsers = 0;
        }
        if (status == null) {
            status = RecommendationRebuildStatus.SCHEDULED;
        }
    }
}
