package com.example.event.dto;

import com.example.event.model.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDto {
    private Long id;

    @NotNull
    private Long userId;

    @NotNull
    private Long itemId;

    @NotNull
    private EventType type;

    private Instant createdAt;
}
