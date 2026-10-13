package app.sanctuary.api.config;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;

public final class CognitoTokenValidators {
    private CognitoTokenValidators() {}

    public static OAuth2TokenValidator<Jwt> forClient(String issuer, String client) {
        if (issuer == null || issuer.isBlank() || client == null || client.isBlank())
            throw new IllegalStateException("Authentication requires a Cognito issuer and app client");
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), token -> {
            String use = token.getClaimAsString("token_use");
            boolean valid = "id".equals(use) && token.getAudience().equals(java.util.List.of(client))
                || "access".equals(use) && client.equals(token.getClaimAsString("client_id"));
            return valid && token.getExpiresAt() != null && token.getSubject() != null && !token.getSubject().isBlank()
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid Cognito client or token type", null));
        });
    }
}
