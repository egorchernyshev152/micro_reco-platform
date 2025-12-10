package com.example.event.mapper;

import com.example.event.dto.EventDto;
import com.example.event.entity.Event;
import com.example.event.model.EventModel;

public final class EventMapper {
    private EventMapper() {
    }

    public static EventModel toModel(Event entity) {
        // переводим entity события в внутреннюю модель
        if (entity == null) return null;
        return EventModel.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .itemId(entity.getItemId())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static Event toEntity(EventModel model) {
        // собираем entity для сохранения в БД
        if (model == null) return null;
        return Event.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .itemId(model.getItemId())
                .type(model.getType())
                .createdAt(model.getCreatedAt())
                .build();
    }

    public static EventModel fromDto(EventDto dto) {
        // конвертируем DTO в модель
        if (dto == null) return null;
        return EventModel.builder()
                .id(dto.getId())
                .userId(dto.getUserId())
                .itemId(dto.getItemId())
                .type(dto.getType())
                .createdAt(dto.getCreatedAt())
                .build();
    }

    public static EventDto toDto(EventModel model) {
        // конвертируем модель в DTO для ответа
        if (model == null) return null;
        return EventDto.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .itemId(model.getItemId())
                .type(model.getType())
                .createdAt(model.getCreatedAt())
                .build();
    }
}
