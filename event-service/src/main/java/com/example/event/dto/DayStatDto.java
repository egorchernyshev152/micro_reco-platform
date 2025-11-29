package com.example.event.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayStatDto {
    private String day;
    private Long count;
}

