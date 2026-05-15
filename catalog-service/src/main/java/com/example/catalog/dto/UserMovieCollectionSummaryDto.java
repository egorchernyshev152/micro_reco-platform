package com.example.catalog.dto;

import com.example.catalog.entity.UserMovieCollectionType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class UserMovieCollectionSummaryDto {
    Long movieId;
    List<UserMovieCollectionType> types;
}

