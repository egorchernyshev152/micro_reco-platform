package com.example.catalog.integration.event;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class EventRequest {
    Long userId;
    Long movieId;
    Long actorId;
    String type;
    String source;
    String device;
    String actorName;
    Map<String, Object> payload;
}
