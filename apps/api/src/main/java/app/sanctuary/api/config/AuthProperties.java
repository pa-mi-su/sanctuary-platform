package app.sanctuary.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.auth")
public record AuthProperties(
    boolean enabled,
    String audience
) {
}
