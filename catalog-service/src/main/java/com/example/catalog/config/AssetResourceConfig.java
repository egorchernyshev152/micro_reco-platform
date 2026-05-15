package com.example.catalog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class AssetResourceConfig implements WebMvcConfigurer {

    private final MovieAssetProperties assetProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (assetProperties.getStorageType() != MovieAssetProperties.StorageType.LOCAL) {
            return;
        }
        String handlerPattern = resolveHandlerPattern(assetProperties.getLocal().getPublicUrl());
        Path root = Path.of(assetProperties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler(handlerPattern)
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }

    private String resolveHandlerPattern(String publicUrl) {
        String path = publicUrl;
        try {
            URI uri = URI.create(publicUrl);
            if (StringUtils.hasText(uri.getPath())) {
                path = uri.getPath();
            }
        } catch (IllegalArgumentException ignored) {
        }
        if (!StringUtils.hasText(path)) {
            path = "/assets";
        }
        if (!path.endsWith("/")) {
            path += "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path + "**";
    }
}
