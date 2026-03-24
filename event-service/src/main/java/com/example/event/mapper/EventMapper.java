package com.example.event.mapper;

import com.example.event.dto.EventDto;
import com.example.event.entity.Event;
import com.example.event.model.EventModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

public final class EventMapper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private EventMapper() {
    }

    public static EventModel toModel(Event entity) {
        if (entity == null) return null;
        return EventModel.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .movieId(entity.getMovieId())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .sessionId(entity.getSessionId())
                .source(entity.getSource())
                .device(entity.getDevice())
                .payload(fromJson(entity.getPayload()))
                .build();
    }

    public static Event toEntity(EventModel model) {
        if (model == null) return null;
        return Event.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .movieId(model.getMovieId())
                .type(model.getType())
                .createdAt(model.getCreatedAt())
                .sessionId(model.getSessionId())
                .source(model.getSource())
                .device(model.getDevice())
                .payload(toJson(model.getPayload()))
                .build();
    }

    public static EventModel fromDto(EventDto dto) {
        if (dto == null) return null;
        return EventModel.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .movieId(dto.getMovieId())
                .type(dto.getType())
                .createdAt(dto.getCreatedAt())
                .sessionId(dto.getSessionId())
                .source(dto.getSource())
                .device(dto.getDevice())
                .payload(dto.getPayload())
                .build();
    }

    public static EventDto toDto(EventModel model) {
        if (model == null) return null;
        return EventDto.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .movieId(model.getMovieId())
                .type(model.getType())
                .createdAt(model.getCreatedAt())
                .sessionId(model.getSessionId())
                .source(model.getSource())
                .device(model.getDevice())
                .payload(model.getPayload())
                .build();
    }

    private static String toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize payload", e);
        }
    }

    private static Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to deserialize payload", e);
        }
    }
}
