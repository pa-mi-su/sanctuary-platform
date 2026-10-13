package app.sanctuary.api.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    private final AuthProperties authProperties;
    private final CookieBearerTokenResolver cookieBearerTokenResolver;

    public SecurityConfig(AuthProperties authProperties, CookieBearerTokenResolver cookieBearerTokenResolver) {
        this.authProperties = authProperties;
        this.cookieBearerTokenResolver = cookieBearerTokenResolver;
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
            .oauth2ResourceServer(oauth2 -> oauth2
                .bearerTokenResolver(cookieBearerTokenResolver)
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "sanctuary.auth", name = "enabled", havingValue = "true")
    JwtDecoder jwtDecoder(org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties properties) {
        String issuerUri = properties.getJwt().getIssuerUri();
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);
        String client = authProperties.audience();
        if (client == null || client.isBlank()) client = authProperties.clientId();
        decoder.setJwtValidator(CognitoTokenValidators.forClient(issuerUri, client));
        return decoder;
    }
}
