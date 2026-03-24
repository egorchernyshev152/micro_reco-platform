package com.example.event.controller;

import com.example.event.dto.DayStatDto;
import com.example.event.dto.EventDto;
import com.example.event.dto.MovieStatDto;
import com.example.event.dto.TimeDistributionStatDto;
import com.example.event.dto.UserStatDto;
import com.example.event.model.EventType;
import com.example.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Validated
@Tag(name = "Events", description = "Events and statistics")
public class EventController {
    private final EventService eventService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create event")
    public EventDto create(@Valid @RequestBody EventDto dto) {
        return eventService.createEvent(dto);
    }

    @PostMapping("/events/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create events in batch")
    public List<EventDto> createBatch(@RequestBody @Valid @NotEmpty List<EventDto> dtos) {
        return eventService.createEvents(dtos);
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Update event")
    public EventDto update(@PathVariable Long id, @Valid @RequestBody EventDto dto) {
        return eventService.updateEvent(id, dto);
    }

    @GetMapping("/events")
    @Operation(summary = "Get events with filters")
    public List<EventDto> find(@RequestParam(value = "userId", required = false) Long userId,
                               @RequestParam(value = "movieId", required = false) Long movieId,
                               @RequestParam(value = "type", required = false) EventType type,
                               @RequestParam(value = "period", required = false) String period,
                               @RequestParam(value = "source", required = false) String source,
                               @RequestParam(value = "sessionId", required = false) String sessionId,
                               @RequestParam(value = "limit", required = false) Integer limit) {
        return eventService.getEvents(userId, movieId, type, period, source, sessionId, limit);
    }

    @GetMapping("/events/{id}")
    @Operation(summary = "Get event by id")
    public EventDto getById(@PathVariable Long id) {
        return eventService.getEvent(id);
    }

    @GetMapping("/events/stats/by-movie")
    @Operation(summary = "Get movie popularity stats")
    public List<MovieStatDto> statsByMovie(@RequestParam(value = "period", required = false) String period) {
        return eventService.getStatsByMovie(period);
    }

    @GetMapping("/events/stats/by-user")
    @Operation(summary = "Get user activity stats")
    public List<UserStatDto> statsByUser(@RequestParam(value = "period", required = false) String period) {
        return eventService.getStatsByUser(period);
    }

    @GetMapping("/events/stats/by-day")
    @Operation(summary = "Get daily stats")
    public List<DayStatDto> statsByDay(@RequestParam(value = "period", required = false) String period) {
        return eventService.getStatsByDay(period);
    }

    @GetMapping("/events/stats/time-distribution")
    @Operation(summary = "Get hourly distribution")
    public List<TimeDistributionStatDto> statsByHour(@RequestParam(value = "period", required = false) String period) {
        return eventService.getTimeDistribution(period);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete event")
    public void delete(@PathVariable Long id) {
        eventService.deleteEvent(id);
    }
}
