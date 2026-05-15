package com.example.catalog.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.assets")
public class MovieAssetProperties {

    private StorageType storageType = StorageType.LOCAL;
    private Local local = new Local();

    public enum StorageType {
        LOCAL
    }

    @Getter
    @Setter
    public static class Local {
        @NotBlank
        private String baseDir = System.getProperty("user.home") + "/.micro-reco/assets";

        /**
         * Базовый публичный URL (применяется для Link в DTO). Может быть абсолютным http(s) или относительным путем.
         */
        @NotBlank
        private String publicUrl = "http://localhost:8081/assets";
    }

}
