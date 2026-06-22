package app.sanctuary.api.admin.notification;

public class DisabledPushNotificationGateway implements PushNotificationGateway {

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public PushNotificationSendResult send(PushNotificationTarget target, PushNotificationPayload payload) {
        return PushNotificationSendResult.failed("Firebase notifications are not configured.", false);
    }
}
