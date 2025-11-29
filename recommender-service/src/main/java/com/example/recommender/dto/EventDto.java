package com.example.recommender.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private Long id;
    private Long userId;
    private Long itemId;
    private String type;
    private Instant createdAt;
}

