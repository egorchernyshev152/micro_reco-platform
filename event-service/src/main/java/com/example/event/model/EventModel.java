package com.example.event.model;

import com.example.event.model.EventType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class EventModel {
    Long id;
    Long userId;
    Long itemId;
    EventType type;
    Instant createdAt;
}
