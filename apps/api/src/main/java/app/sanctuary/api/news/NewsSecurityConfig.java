package app.sanctuary.api.news;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import app.sanctuary.api.config.AuthProperties;
import app.sanctuary.api.config.CookieBearerTokenResolver;

@Configuration
public class NewsSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain newsAdminSecurity(HttpSecurity http, AuthProperties auth,
                                         CookieBearerTokenResolver resolver, NewsMfaSessions sessions) throws Exception {
        // All admin APIs fail closed; future app-management routes need their own explicit policy.
        http.securityMatcher("/admin", "/admin/**")
            .cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
            .headers(headers -> headers.cacheControl(Customizer.withDefaults()));
        if (!auth.enabled()) {
            http.authorizeHttpRequests(requests -> requests.anyRequest().denyAll());
        } else {
            http.authorizeHttpRequests(requests -> requests.anyRequest().access((authentication, context) -> {
                var principal = authentication.get();
                String path=context.getRequest().getRequestURI();
                boolean newsPath=path.equals("/admin/news") || path.startsWith("/admin/news/");
                boolean admin = newsPath && principal instanceof JwtAuthenticationToken token
                    && token.isAuthenticated()
                    && NewsIdentityPolicy.accepts(token.getToken(), auth);
                boolean safe = List.of("GET", "HEAD", "OPTIONS").contains(context.getRequest().getMethod());
                boolean header = "1".equals(context.getRequest().getHeader("X-Sanctuary-News-Admin"));
                boolean mfa = admin && principal instanceof JwtAuthenticationToken token && sessions.valid(token.getToken().getTokenValue(), context.getRequest());
                return new AuthorizationDecision(admin && mfa && (safe || header));
            })).oauth2ResourceServer(oauth -> oauth.bearerTokenResolver(resolver).jwt(Customizer.withDefaults()));
        }
        return http.build();
    }
}
