package com.example.catalog.service.asset;

import com.example.catalog.config.MovieAssetProperties;
import com.example.catalog.entity.MovieAssetType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(prefix = "app.assets", name = "storage-type", havingValue = "s3")
public class UnsupportedMovieAssetStorage implements MovieAssetStorage {

    @Override
    public MovieAssetProperties.StorageType getType() {
        return MovieAssetProperties.StorageType.LOCAL;
    }

    @Override
    public StoredMovieAsset store(Long movieId, MovieAssetType type, MultipartFile file) {
        throw new MovieAssetStorageException("S3 storage недоступно в этой сборке. Используйте storage-type=local.");
    }
}
