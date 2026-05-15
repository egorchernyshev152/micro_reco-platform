package com.example.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarUsersResponse {
    private Long userId;
    private String period;
    private Instant generatedAt;
    private List<SimilarUserDto> items;
}
