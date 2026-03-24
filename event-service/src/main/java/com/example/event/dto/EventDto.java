package com.example.event.dto;

import com.example.event.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    private Long movieId;

    @NotNull
    private EventType type;

    private Instant createdAt;

    /**
     * Клиентский session-id (для сгруппированных действий).
     */
    private String sessionId;

    /**
     * Источник события (web, ios, android, smart-tv).
     */
    @NotBlank
    private String source;

    private String device;

    /**
     * Произвольный payload (например, {\"position\":120} для WATCH_TRAILER).
     */
    private Map<String, Object> payload;
}
