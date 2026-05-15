package com.example.recommender.model;

import com.example.recommender.converter.StringSetJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "user_recommendation_preferences")
@Getter
@Setter
public class UserRecommendationPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Convert(converter = StringSetJsonConverter.class)
    @Column(name = "boost_genres")
    private Set<String> boostGenres = new LinkedHashSet<>();

    @Convert(converter = StringSetJsonConverter.class)
    @Column(name = "mute_genres")
    private Set<String> muteGenres = new LinkedHashSet<>();

    @Column(name = "freshness_bias", nullable = false)
    private double freshnessBias = 0.5;

    @Column(name = "discovery_bias", nullable = false)
    private double discoveryBias = 0.5;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
