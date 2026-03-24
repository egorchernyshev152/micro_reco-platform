package com.example.event.service;

import com.example.event.dto.DayStatDto;
import com.example.event.dto.EventDto;
import com.example.event.dto.MovieStatDto;
import com.example.event.dto.TimeDistributionStatDto;
import com.example.event.dto.UserStatDto;
import com.example.event.entity.Event;
import com.example.event.exception.NotFoundException;
import com.example.event.mapper.EventMapper;
import com.example.event.model.EventType;
import com.example.event.repository.EventRepository;
import com.example.event.repository.spec.EventSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private static final int DEFAULT_LIMIT = 500;

    private final EventRepository eventRepository;

    @Transactional
    public EventDto createEvent(EventDto dto) {
        Event saved = eventRepository.save(EventMapper.toEntity(EventMapper.fromDto(dto)));
        return EventMapper.toDto(EventMapper.toModel(saved));
    }

    @Transactional
    public List<EventDto> createEvents(List<EventDto> dtos) {
        List<Event> entities = dtos.stream()
                .map(EventMapper::fromDto)
                .map(EventMapper::toEntity)
                .toList();
        List<Event> saved = eventRepository.saveAll(entities);
        return saved.stream()
                .map(EventMapper::toModel)
                .map(EventMapper::toDto)
                .toList();
    }

    @Transactional
    public EventDto updateEvent(Long id, EventDto dto) {
        Event existing = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
        Event updated = EventMapper.toEntity(EventMapper.fromDto(dto));
        updated.setId(id);
        updated.setCreatedAt(existing.getCreatedAt());
        Event saved = eventRepository.save(updated);
        return EventMapper.toDto(EventMapper.toModel(saved));
    }

    public List<EventDto> getEvents(Long userId,
                                    Long movieId,
                                    EventType type,
                                    String period,
                                    String source,
                                    String sessionId,
                                    Integer limit) {
        Instant from = resolveFrom(period);
        Specification<Event> spec = Specification.where(EventSpecifications.userId(userId))
                .and(EventSpecifications.movieId(movieId))
                .and(EventSpecifications.type(type))
                .and(EventSpecifications.createdAfter(from))
                .and(EventSpecifications.source(source))
                .and(EventSpecifications.sessionId(sessionId));
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, 1000));
        return eventRepository.findAll(spec, PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(EventMapper::toModel)
                .map(EventMapper::toDto)
                .toList();
    }

    public EventDto getEvent(Long id) {
        return eventRepository.findById(id)
                .map(EventMapper::toModel)
                .map(EventMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Event not found: " + id));
    }

    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new NotFoundException("Event not found: " + id);
        }
        eventRepository.deleteById(id);
    }

    public List<MovieStatDto> getStatsByMovie(String period) {
        Instant from = resolveFrom(period);
        return eventRepository.countByMovie(from).stream()
                .map(row -> MovieStatDto.builder()
                        .movieId(((Number) row[0]).longValue())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    public List<UserStatDto> getStatsByUser(String period) {
        Instant from = resolveFrom(period);
        return eventRepository.countByUser(from).stream()
                .map(row -> UserStatDto.builder()
                        .userId(((Number) row[0]).longValue())
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    public List<DayStatDto> getStatsByDay(String period) {
        Instant from = resolveFrom(period);
        return eventRepository.countByDay(from).stream()
                .map(row -> DayStatDto.builder()
                        .day((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    public List<TimeDistributionStatDto> getTimeDistribution(String period) {
        Instant from = resolveFrom(period);
        return eventRepository.countByHour(from).stream()
                .map(row -> TimeDistributionStatDto.builder()
                        .bucket((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
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
}
