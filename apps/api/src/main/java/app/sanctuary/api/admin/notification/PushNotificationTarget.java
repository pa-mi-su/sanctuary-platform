package app.sanctuary.api.admin.notification;

import java.util.UUID;

public record PushNotificationTarget(
    UUID deviceId,
    UUID userId,
    String platform,
    String token
) {}
