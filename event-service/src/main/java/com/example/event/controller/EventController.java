package com.example.event.controller;

import com.example.event.dto.DayStatDto;
import com.example.event.dto.EventDto;
import com.example.event.dto.ItemStatDto;
import com.example.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Events", description = "Events and statistics")
public class EventController {
    private final EventService eventService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create event")
    public EventDto create(@Valid @RequestBody EventDto dto) {
        return eventService.createEvent(dto);
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Update event")
    public EventDto update(@PathVariable Long id, @Valid @RequestBody EventDto dto) {
        return eventService.updateEvent(id, dto);
    }

    @GetMapping("/events")
    @Operation(summary = "Get events (optionally filtered by user)")
    public List<EventDto> byUser(@RequestParam(value = "userId", required = false) Long userId) {
        return eventService.getEvents(userId);
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "Get event by id")
    public EventDto getById(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @GetMapping("/events/stats/by-item")
    @Operation(summary = "Get item popularity stats")
    public List<ItemStatDto> statsByItem(@RequestParam(value = "period", required = false) String period) {
        return eventService.getStatsByItem(period);
    }

    @GetMapping("/events/stats/by-day")
    @Operation(summary = "Get daily stats")
    public List<DayStatDto> statsByDay(@RequestParam(value = "period", required = false) String period) {
        return eventService.getStatsByDay(period);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete event")
    public void delete(@PathVariable Long id) {
        eventService.deleteEvent(id);
    }
}
