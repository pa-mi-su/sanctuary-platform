package app.sanctuary.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.auth")
public record AuthProperties(
    boolean enabled,
    String audience,
    String userPoolId,
    String clientId,
    Cookie cookie
) {
    public AuthProperties(boolean enabled, String audience, String userPoolId, String clientId) {
        this(enabled, audience, userPoolId, clientId, null);
    }

    public Cookie cookie() {
        return cookie == null ? new Cookie(null, true, "None") : cookie;
    }

    public record Cookie(
        String domain,
        boolean secure,
        String sameSite
    ) {
        public String sameSite() {
            return sameSite == null || sameSite.isBlank() ? "None" : sameSite;
        }
    }
}
