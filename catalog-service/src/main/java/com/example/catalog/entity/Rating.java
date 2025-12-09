package com.example.catalog.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "ratings", uniqueConstraints = @UniqueConstraint(name = "uq_rating_user_item", columnNames = {"user_id", "item_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rating_user"))
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rating_item"))
    private Item item;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
