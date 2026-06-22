package app.sanctuary.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(WebProperties.class)
public class WebConfig implements WebMvcConfigurer {
    static final String[] DEFAULT_ALLOWED_ORIGINS = {
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "https://mydailysanctuary.com",
        "https://www.mydailysanctuary.com"
    };

    private final WebProperties webProperties;

    public WebConfig(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins())
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }

    String[] allowedOrigins() {
        if (webProperties.allowedOrigins().isEmpty()) {
            return DEFAULT_ALLOWED_ORIGINS;
        }

        return webProperties.allowedOrigins().toArray(String[]::new);
    }

    @Bean
    OriginEnforcementFilter originEnforcementFilter() {
        return new OriginEnforcementFilter(webProperties);
    }
}
