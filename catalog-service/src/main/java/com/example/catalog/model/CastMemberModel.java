package com.example.catalog.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CastMemberModel {
    Long tmdbId;
    String name;
    String character;
    String profileUrl;
    Integer orderIndex;
}

