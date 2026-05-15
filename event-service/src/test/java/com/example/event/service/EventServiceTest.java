package com.example.event.service;

import com.example.event.dto.EventDto;
import com.example.event.exception.NotFoundException;
import com.example.event.model.EventType;
import com.example.event.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void createEventShouldPersist() {
        when(eventRepository.save(any())).thenAnswer(invocation -> {
            com.example.event.entity.Event entity = invocation.getArgument(0);
            entity.setId(10L);
            entity.setCreatedAt(Instant.EPOCH);
            return entity;
        });

        EventDto dto = EventDto.builder()
                .userId(1L)
                .movieId(2L)
                .source("web")
                .type(EventType.VIEW_CARD)
                .build();

        EventDto saved = eventService.createEvent(dto);

        assertThat(saved.getId()).isEqualTo(10L);
        verify(eventRepository).save(any());
    }

    @Test
    void createEventsShouldPersistBatch() {
        when(eventRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<EventDto> dtos = List.of(
                EventDto.builder().userId(1L).movieId(1L).source("web").type(EventType.VIEW_CARD).build(),
                EventDto.builder().userId(2L).movieId(2L).source("ios").type(EventType.WATCH_TRAILER).build()
        );

        List<EventDto> saved = eventService.createEvents(dtos);

        assertThat(saved).hasSize(2);
        verify(eventRepository).saveAll(anyList());
    }

    @Test
    void updateEventShouldCopyCreatedAt() {
        var existing = new com.example.event.entity.Event();
        existing.setId(5L);
        existing.setCreatedAt(Instant.EPOCH);
        when(eventRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EventDto updated = eventService.updateEvent(5L, EventDto.builder()
                .userId(2L)
                .movieId(3L)
                .source("android")
                .type(EventType.FAVORITE)
                .build());

        assertThat(updated.getCreatedAt()).isEqualTo(Instant.EPOCH);
        verify(eventRepository).save(any());
    }

    @Test
    void updateEventShouldThrowWhenMissing() {
        when(eventRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEvent(100L, EventDto.builder().build()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getEventsShouldBuildSpecification() {
        when(eventRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        eventService.getEvents(1L, 2L, EventType.VIEW_CARD, "week", "web", "s1", 10);

        verify(eventRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void getEventShouldThrowWhenMissing() {
        when(eventRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(404L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteEventShouldValidateExistence() {
        when(eventRepository.existsById(7L)).thenReturn(true);

        eventService.deleteEvent(7L);

        verify(eventRepository).deleteById(7L);
    }

    @Test
    void statsShouldQueryRepository() {
        when(eventRepository.countByMovie(null)).thenReturn(java.util.Collections.singletonList(new Object[]{1L, 5L}));
        when(eventRepository.countByUser(null)).thenReturn(java.util.Collections.singletonList(new Object[]{2L, 3L}));
        when(eventRepository.countByDay(null)).thenReturn(java.util.Collections.singletonList(new Object[]{"2024-01-01", 7L}));
        when(eventRepository.countByHour(null)).thenReturn(java.util.Collections.singletonList(new Object[]{"2024-01-01 10:00", 4L}));

        assertThat(eventService.getStatsByMovie(null)).hasSize(1);
        assertThat(eventService.getStatsByUser(null)).hasSize(1);
        assertThat(eventService.getStatsByDay(null)).hasSize(1);
        assertThat(eventService.getTimeDistribution(null)).hasSize(1);
    }
}
