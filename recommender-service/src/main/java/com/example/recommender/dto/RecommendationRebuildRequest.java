package com.example.recommender.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecommendationRebuildRequest {
    @Size(max = 255)
    private String initiator;
}
