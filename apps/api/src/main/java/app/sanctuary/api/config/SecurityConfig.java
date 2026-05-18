package app.sanctuary.api.config;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties({ AuthProperties.class, AuthAbuseProtectionProperties.class })
public class SecurityConfig {

    private final AuthProperties authProperties;

    public SecurityConfig(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults());

        if (!authProperties.enabled()) {
            http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
            return http.build();
        }

        http
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/health", "/actuator/**", "/auth/**", "/calendar/**", "/content/**").permitAll()
                .requestMatchers("/me/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "sanctuary.auth", name = "enabled", havingValue = "true")
    JwtDecoder jwtDecoder(org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties properties) {
        String issuerUri = properties.getJwt().getIssuerUri();
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);

        if (authProperties.audience() == null || authProperties.audience().isBlank()) {
            decoder.setJwtValidator(issuerValidator);
            return decoder;
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            issuerValidator,
            token -> {
                List<String> audience = token.getAudience();
                String clientId = token.getClaimAsString("client_id");
                boolean valid = (audience != null && audience.contains(authProperties.audience()))
                    || authProperties.audience().equals(clientId);
                return valid
                    ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                    : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error(
                            "invalid_token",
                            "JWT audience does not match the configured Cognito app client.",
                            null
                        )
                    );
            }
        ));
        return decoder;
    }
}
