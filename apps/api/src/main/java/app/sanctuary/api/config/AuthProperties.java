package app.sanctuary.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.auth")
public class AuthProperties {
    private boolean enabled;
    private String audience;
    private String userPoolId;
    private String clientId;
    private Cookie cookie = new Cookie();

    public AuthProperties() {
    }

    public AuthProperties(boolean enabled, String audience, String userPoolId, String clientId) {
        this.enabled = enabled;
        this.audience = audience;
        this.userPoolId = userPoolId;
        this.clientId = clientId;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String audience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String userPoolId() {
        return userPoolId;
    }

    public void setUserPoolId(String userPoolId) {
        this.userPoolId = userPoolId;
    }

    public String clientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Cookie cookie() {
        return cookie == null ? new Cookie() : cookie;
    }

    public void setCookie(Cookie cookie) {
        this.cookie = cookie == null ? new Cookie() : cookie;
    }

    public static class Cookie {
        private String domain;
        private boolean secure = true;
        private String sameSite = "None";

        public String domain() {
            return domain;
        }

        public void setDomain(String domain) {
            this.domain = domain;
        }

        public boolean secure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String sameSite() {
            return sameSite == null || sameSite.isBlank() ? "None" : sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
        }
    }
}
