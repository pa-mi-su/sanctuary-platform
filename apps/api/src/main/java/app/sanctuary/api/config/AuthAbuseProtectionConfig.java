package app.sanctuary.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthAbuseProtectionProperties.class)
public class AuthAbuseProtectionConfig {

    @Bean
    AuthAbuseProtectionFilter authAbuseProtectionFilter(
        AuthAbuseProtectionProperties properties,
        ObjectMapper objectMapper
    ) {
        return new AuthAbuseProtectionFilter(properties, objectMapper);
    }
}
