package com.example.event.dto;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    private String type;

    private Instant createdAt;
}

