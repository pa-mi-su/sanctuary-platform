package app.sanctuary.api.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.notifications.firebase")
public record FirebaseNotificationProperties(
    boolean enabled,
    String appName,
    String serviceAccountJson
) {
    public String resolvedAppName() {
        return appName == null || appName.isBlank() ? "sanctuary-api" : appName.trim();
    }
}
