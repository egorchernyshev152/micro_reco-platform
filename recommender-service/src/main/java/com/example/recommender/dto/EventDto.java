package com.example.recommender.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private Long id;
    private Long userId;
    private Long movieId;
    private String type;
    private Instant createdAt;
    private String sessionId;
    private String source;
    private String device;
    private Map<String, Object> payload;
}

