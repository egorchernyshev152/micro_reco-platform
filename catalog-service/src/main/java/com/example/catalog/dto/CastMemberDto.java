package com.example.catalog.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CastMemberDto {
    Long tmdbId;
    String name;
    String character;
    String profileUrl;
    Integer orderIndex;
}

