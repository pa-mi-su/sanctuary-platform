package app.sanctuary.api.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("sanctuary.admin")
public record AdminProperties(boolean enabled, String ownerSub, String clientId, String encryptionKey) {
    // The email is checked only in addition to the immutable, operator-pinned Cognito subject.
    public static final String OWNER_EMAIL = "pmsulikowski@icloud.com";
    public boolean configured() {
        return enabled && clientId != null && !clientId.isBlank() && ownerSub != null && !ownerSub.isBlank()
            && encryptionKey != null && !encryptionKey.isBlank();
    }
}
