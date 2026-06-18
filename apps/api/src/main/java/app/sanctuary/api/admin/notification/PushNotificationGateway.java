package app.sanctuary.api.admin.notification;

public interface PushNotificationGateway {
    boolean enabled();

    PushNotificationSendResult send(PushNotificationTarget target, PushNotificationPayload payload);
}
