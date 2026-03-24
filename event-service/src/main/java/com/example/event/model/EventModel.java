package com.example.event.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class EventModel {
    Long id;
    Long userId;
    Long movieId;
    EventType type;
    Instant createdAt;
    String sessionId;
    String source;
    String device;
    Map<String, Object> payload;
}
