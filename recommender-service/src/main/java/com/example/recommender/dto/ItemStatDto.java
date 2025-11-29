package com.example.recommender.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemStatDto {
    private Long itemId;
    private Long count;
}

