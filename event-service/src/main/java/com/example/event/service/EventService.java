package com.example.event.service;

import com.example.event.dto.DayStatDto;
import com.example.event.dto.EventDto;
import com.example.event.dto.ItemStatDto;
import com.example.event.entity.Event;
import com.example.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;

    public EventDto createEvent(EventDto dto) {
        Event e = Event.builder()
                .userId(dto.getUserId())
                .itemId(dto.getItemId())
                .type(dto.getType())
                .build();
        e = eventRepository.save(e);
        return toDto(e);
    }

    public List<EventDto> getEventsByUser(Long userId) {
        return eventRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    public List<ItemStatDto> getStatsByItem(String period) {
        Instant from = resolveFrom(period);
        List<Object[]> rows = from == null ? eventRepository.countByItemAll() : eventRepository.countByItemSince(from);
        return rows.stream()
                .map(r -> ItemStatDto.builder().itemId(((Number) r[0]).longValue()).count(((Number) r[1]).longValue()).build())
                .toList();
    }

    public List<DayStatDto> getStatsByDay(String period) {
        Instant from = resolveFrom(period);
        List<Object[]> rows = from == null ? eventRepository.countByDayAll() : eventRepository.countByDaySince(from);
        return rows.stream()
                .map(r -> DayStatDto.builder().day((String) r[0]).count(((Number) r[1]).longValue()).build())
                .toList();
    }

    private Instant resolveFrom(String period) {
        if (period == null || period.isBlank()) return null;
        return switch (period.toUpperCase()) {
            case "DAY" -> Instant.now().minus(1, ChronoUnit.DAYS);
            case "WEEK" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "MONTH" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> null;
        };
    }

    private EventDto toDto(Event e) {
        return EventDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .itemId(e.getItemId())
                .type(e.getType())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

