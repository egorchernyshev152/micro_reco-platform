package com.example.recommender.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class UserPreferenceRequest {
    @Size(max = 5, message = "boostGenres limit is 5")
    private Set<String> boostGenres = new LinkedHashSet<>();

    @Size(max = 5, message = "muteGenres limit is 5")
    private Set<String> muteGenres = new LinkedHashSet<>();

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double freshnessBias;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double discoveryBias;
}
