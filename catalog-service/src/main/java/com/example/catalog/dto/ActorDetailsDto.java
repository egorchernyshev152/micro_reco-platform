package com.example.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActorDetailsDto {
    private Long tmdbId;
    private String name;
    private String biography;
    private String birthday;
    private String deathday;
    private String placeOfBirth;
    private String profileUrl;
    private String knownForDepartment;
    private Double popularity;
    @Builder.Default
    private List<String> alsoKnownAs = new ArrayList<>();
    @Builder.Default
    private List<String> highlights = new ArrayList<>();
    @Builder.Default
    private List<ActorKnownForDto> knownFor = new ArrayList<>();
}
