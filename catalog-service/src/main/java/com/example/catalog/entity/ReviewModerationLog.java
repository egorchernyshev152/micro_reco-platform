package com.example.catalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "review_moderation_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false, foreignKey = @ForeignKey(name = "fk_review_log_review"))
    private Review review;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private ReviewModerationAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewStatus status;

    @Column(length = 512)
    private String reason;

    @Column(name = "performed_by_id")
    private Long performedById;

    @Column(name = "performed_by_email")
    private String performedByEmail;

    @Column(name = "performed_by_name")
    private String performedByName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
