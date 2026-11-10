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
    SecurityFilterChain adminSecurity(HttpSecurity http, AdminAuthService auth, com.fasterxml.jackson.databind.ObjectMapper json, AdminSessionCookies cookies) throws Exception {
        return http.securityMatcher("/admin-api/**")
            .csrf(csrf -> csrf.disable()) // Custom header and exact-origin CORS protect cookie-backed requests from CSRF.
            .cors(Customizer.withDefaults())
            .addFilterBefore(new AdminSessionFilter(auth, json, cookies), org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
            .headers(headers -> headers.cacheControl(Customizer.withDefaults()))
            .build();
    }
}
