package app.sanctuary.api.admin;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(AdminProperties.class)
public class AdminSecurityConfig {
    // Independent of the public application's optional JWT authentication switch.
    // Controllers validate opaque server sessions, never ordinary Cognito bearer tokens.
    @Bean @Order(1)
    SecurityFilterChain adminSecurity(HttpSecurity http, AdminAuthService auth, com.fasterxml.jackson.databind.ObjectMapper json) throws Exception {
        return http.securityMatcher("/admin-api/**")
            .csrf(csrf -> csrf.disable()) // No ambient credentials: explicit in-memory bearer + custom header.
            .cors(Customizer.withDefaults())
            .addFilterBefore(new AdminSessionFilter(auth, json), org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
            .headers(headers -> headers.cacheControl(Customizer.withDefaults()))
            .build();
    }
}
