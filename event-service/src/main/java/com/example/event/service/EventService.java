package com.example.event.service;

import com.example.event.dto.DayStatDto;
import com.example.event.dto.EventDto;
import com.example.event.dto.ItemStatDto;
import com.example.event.entity.Event;
import com.example.event.exception.DuplicateEventException;
import com.example.event.exception.NotFoundException;
import com.example.event.mapper.EventMapper;
import com.example.event.model.EventModel;
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
        EventModel model = EventMapper.fromDto(dto);
        ensureUnique(model);
        Event saved = eventRepository.save(EventMapper.toEntity(model));
        return EventMapper.toDto(EventMapper.toModel(saved));
    }

    public EventDto updateEvent(Long id, EventDto dto) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
        EventModel model = EventMapper.fromDto(dto);
        ensureUnique(model, id);
        Event toSave = EventMapper.toEntity(EventModel.builder()
                .id(id)
                .userId(model.getUserId())
                .itemId(model.getItemId())
                .type(model.getType())
                .createdAt(existing.getCreatedAt())
                .build());
        Event saved = eventRepository.save(toSave);
        return EventMapper.toDto(EventMapper.toModel(saved));
    }

    public List<EventDto> getEvents(Long userId) {
        List<Event> events = userId == null ? eventRepository.findAll() : eventRepository.findByUserId(userId);
        return events.stream().map(EventMapper::toModel).map(EventMapper::toDto).toList();
    }

    public EventDto getEvent(Long id) {
        return eventRepository.findById(id)
                .map(EventMapper::toModel)
                .map(EventMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
    }

    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new NotFoundException("Event not found: " + id);
        }
        eventRepository.deleteById(id);
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

    private void ensureUnique(EventModel model) {
        if (eventRepository.existsByUserIdAndItemIdAndType(model.getUserId(), model.getItemId(), model.getType())) {
            throw new DuplicateEventException("Event already exists for user " + model.getUserId() +
                    " item " + model.getItemId() + " type " + model.getType());
        }
    }

    private void ensureUnique(EventModel model, Long id) {
        if (eventRepository.existsByUserIdAndItemIdAndTypeAndIdNot(model.getUserId(), model.getItemId(), model.getType(), id)) {
            throw new DuplicateEventException("Event already exists for user " + model.getUserId() +
                    " item " + model.getItemId() + " type " + model.getType());
        }
    }
}
