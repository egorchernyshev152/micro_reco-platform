package com.example.catalog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    /**
     * Signing secret for JWT tokens.
     */
    private String secret;
    /**
     * Token issuer label.
     */
    private String issuer = "reco-platform";
    /**
     * Expiration in seconds.
     */
    private long expirationSeconds = 3600;
}
