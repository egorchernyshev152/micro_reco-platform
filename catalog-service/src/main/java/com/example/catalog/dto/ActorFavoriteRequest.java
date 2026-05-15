package com.example.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActorFavoriteRequest {
    @NotBlank
    private String actorName;
    private String profileUrl;
}
