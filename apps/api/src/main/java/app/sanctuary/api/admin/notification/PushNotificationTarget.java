package app.sanctuary.api.admin.notification;

import java.util.UUID;

public record PushNotificationTarget(
    UUID deviceId,
    String anonymousDeviceId,
    UUID userId,
    String platform,
    String token
) {}
