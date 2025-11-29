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

    @GetMapping("/events")
    @Operation(summary = "Get events by user")
    public List<EventDto> byUser(@RequestParam("userId") Long userId) {
        return eventService.getEventsByUser(userId);
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
}

