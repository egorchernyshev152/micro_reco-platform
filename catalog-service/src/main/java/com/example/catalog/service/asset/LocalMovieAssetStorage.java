package com.example.catalog.service.asset;

import com.example.catalog.config.MovieAssetProperties;
import com.example.catalog.entity.MovieAssetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.assets", name = "storage-type", havingValue = "local", matchIfMissing = true)
public class LocalMovieAssetStorage implements MovieAssetStorage {

    private final MovieAssetProperties properties;
    private final Path root;

    public LocalMovieAssetStorage(MovieAssetProperties properties) {
        this.properties = properties;
        this.root = Path.of(properties.getLocal().getBaseDir()).toAbsolutePath().normalize();
    }

    @Override
    public MovieAssetProperties.StorageType getType() {
        return MovieAssetProperties.StorageType.LOCAL;
    }

    @Override
    public StoredMovieAsset store(Long movieId, MovieAssetType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MovieAssetStorageException("Файл не найден в запросе");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        String fileName = buildFileName(type, extension);
        Path movieDir = root.resolve("movies").resolve(String.valueOf(movieId));
        try {
            Files.createDirectories(movieDir);
            Files.copy(file.getInputStream(), movieDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new MovieAssetStorageException("Не удалось сохранить файл локально", e);
        }
        String objectKey = "movies/" + movieId + "/" + fileName;
        String url = buildPublicUrl(objectKey);
        log.debug("Сохранили локальный ассет {} для фильма {}", objectKey, movieId);
        return new StoredMovieAsset(objectKey, url);
    }

    private String buildFileName(MovieAssetType type, String extension) {
        String base = type.name().toLowerCase(Locale.ROOT) + "-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        return extension != null ? base + "." + extension : base;
    }

    private String resolveExtension(String originalName) {
        if (!StringUtils.hasText(originalName)) {
            return null;
        }
        String normalized = Normalizer.normalize(originalName, Normalizer.Form.NFD);
        int dot = normalized.lastIndexOf('.');
        if (dot == -1 || dot == normalized.length() - 1) {
            return null;
        }
        return normalized.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String buildPublicUrl(String objectKey) {
        String prefix = properties.getLocal().getPublicUrl();
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        return prefix + objectKey;
    }
}
