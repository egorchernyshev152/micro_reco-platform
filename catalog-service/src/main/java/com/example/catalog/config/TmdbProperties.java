package com.example.catalog.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;
import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "tmdb")
public class TmdbProperties {
    /**
     * Ключ TMDb API v3. Ожидается, что он будет передан через переменную окружения или секрет.
     */
    @NotBlank
    private String apiKey;

    /**
     * Базовый URL API TMDb. Обычно https://api.themoviedb.org/3
     */
    @NotBlank
    private String baseUrl = "https://api.themoviedb.org/3";

    /**
     * Базовый URL для изображений TMDb. Обычно https://image.tmdb.org/t/p
     */
    @NotBlank
    private String imageBaseUrl = "https://image.tmdb.org/t/p";

    /**
     * Размер постеров (w342, w500 и т.п.)
     */
    @NotBlank
    private String posterSize = "w500";

    /**
     * Размер backdrop-изображений.
     */
    @NotBlank
    private String backdropSize = "w780";

    /**
     * Размер изображений персон.
     */
    @NotBlank
    private String profileSize = "w185";

    /**
     * Необязательные настройки HTTP-прокси для TMDb.
     */
    private ProxySettings proxy = new ProxySettings();

    /**
     * Максимальное время TLS-рукопожатия (по умолчанию 20 секунд).
     */
    private Duration handshakeTimeout = Duration.ofSeconds(20);

    public boolean isProxyEnabled() {
        return proxy != null
                && StringUtils.hasText(proxy.getHost())
                && proxy.getPort() != null
                && proxy.getPort() > 0;
    }

    @Getter
    @Setter
    public static class ProxySettings {
        /**
         * Хост HTTP-прокси (например, 127.0.0.1).
         */
        private String host;

        /**
         * Порт HTTP-прокси (например, 10809).
         */
        private Integer port;

        /**
         * Необязательные логин/пароль для прокси.
         */
        private String username;
        private String password;
    }

    public String buildPosterUrl(String path) {
        return buildImageUrl(path, posterSize);
    }

    public String buildBackdropUrl(String path) {
        return buildImageUrl(path, backdropSize);
    }

    public String buildProfileUrl(String path) {
        return buildImageUrl(path, profileSize);
    }

    private String buildImageUrl(String path, String size) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalizedBase = imageBaseUrl.endsWith("/") ? imageBaseUrl.substring(0, imageBaseUrl.length() - 1) : imageBaseUrl;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return normalizedBase + "/" + size + path;
    }
}
