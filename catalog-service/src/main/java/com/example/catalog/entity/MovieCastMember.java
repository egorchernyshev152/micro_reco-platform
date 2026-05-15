package com.example.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieCastMember {

    @Column(name = "person_tmdb_id")
    private Long personTmdbId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "character")
    private String character;

    @Column(name = "profile_url")
    private String profileUrl;

    @Column(name = "order_index")
    private Integer orderIndex;
}

