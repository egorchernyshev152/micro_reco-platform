package com.example.catalog.mapper;

import com.example.catalog.dto.MovieAssetDto;
import com.example.catalog.entity.MovieAsset;

public final class MovieAssetMapper {

    private MovieAssetMapper() {
    }

    public static MovieAssetDto toDto(MovieAsset asset) {
        if (asset == null) {
            return null;
        }
        return MovieAssetDto.builder()
                .id(asset.getId())
                .movieId(asset.getMovie() != null ? asset.getMovie().getId() : null)
                .type(asset.getType())
                .url(asset.getPublicUrl())
                .fileName(asset.getFileName())
                .contentType(asset.getContentType())
                .size(asset.getFileSize())
                .storage(asset.getStorage())
                .label(asset.getLabel())
                .createdAt(asset.getCreatedAt())
                .build();
    }
}
