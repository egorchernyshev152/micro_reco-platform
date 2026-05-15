package com.example.catalog.dto;

import com.example.catalog.entity.MovieAssetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
@Schema(description = "Uploaded movie asset metadata")
public class MovieAssetDto {
    Long id;
    Long movieId;
    MovieAssetType type;
    String url;
    String fileName;
    String contentType;
    Long size;
    String storage;
    String label;
    Instant createdAt;
}
