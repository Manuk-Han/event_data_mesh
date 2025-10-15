package module.platform.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "platform.security.jwt")
public record SecurityProps(
        String issuer,
        String hmacSecret,
        long ttlSeconds
) {
    public SecurityProps {
        if (issuer == null || issuer.isBlank()) throw new IllegalArgumentException("issuer required");
        if (hmacSecret == null || hmacSecret.length() < 32) {
            throw new IllegalArgumentException("hmacSecret must be >= 32 chars");
        }
        if (ttlSeconds <= 0) throw new IllegalArgumentException("ttlSeconds must be > 0");
    }
}