package com.example.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferenceDto {
    @Builder.Default
    private Set<String> boostGenres = new LinkedHashSet<>();
    @Builder.Default
    private Set<String> muteGenres = new LinkedHashSet<>();
    private double freshnessBias;
    private double discoveryBias;
    private Long userId;
    private Instant updatedAt;
}
