package app.sanctuary.api.admin.notification;

import java.util.UUID;

public record PushNotificationPayload(
    UUID notificationId,
    String title,
    String message
) {}
