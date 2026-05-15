package com.example.catalog.service.asset;

import com.example.catalog.config.MovieAssetProperties;
import com.example.catalog.entity.MovieAssetType;
import org.springframework.web.multipart.MultipartFile;

public interface MovieAssetStorage {

    MovieAssetProperties.StorageType getType();

    StoredMovieAsset store(Long movieId, MovieAssetType type, MultipartFile file);

    record StoredMovieAsset(String objectKey, String url) {
    }
}
