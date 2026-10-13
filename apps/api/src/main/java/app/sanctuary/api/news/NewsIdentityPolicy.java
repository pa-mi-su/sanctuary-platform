package app.sanctuary.api.news;

import app.sanctuary.api.config.AuthProperties;
import org.springframework.security.oauth2.jwt.Jwt;

/** Admin sessions accept Cognito ID tokens for exactly the configured web client. */
public final class NewsIdentityPolicy {
    private NewsIdentityPolicy() {}

    public static boolean accepts(Jwt jwt, AuthProperties auth) {
        return auth.enabled()
            && auth.clientId() != null && !auth.clientId().isBlank()
            && "id".equals(jwt.getClaimAsString("token_use"))
            && jwt.getAudience().equals(java.util.List.of(auth.clientId()))
            && jwt.getSubject() != null && !jwt.getSubject().isBlank()
            && jwt.getExpiresAt() != null && jwt.getExpiresAt().isAfter(java.time.Instant.now())
            && jwt.getClaimAsStringList("cognito:groups") != null
            && jwt.getClaimAsStringList("cognito:groups").contains("news-admin");
    }
}
