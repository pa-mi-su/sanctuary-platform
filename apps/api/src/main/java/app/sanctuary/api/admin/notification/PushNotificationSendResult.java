package app.sanctuary.api.admin.notification;

public record PushNotificationSendResult(
    boolean sent,
    boolean invalidToken,
    String providerMessageId,
    String failureReason
) {
    public static PushNotificationSendResult sent(String providerMessageId) {
        return new PushNotificationSendResult(true, false, providerMessageId, null);
    }

    public static PushNotificationSendResult failed(String failureReason, boolean invalidToken) {
        return new PushNotificationSendResult(false, invalidToken, null, failureReason);
    }
}
