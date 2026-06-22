package app.sanctuary.api.admin.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

public class FirebasePushNotificationGateway implements PushNotificationGateway {

    private final FirebaseMessaging firebaseMessaging;

    public FirebasePushNotificationGateway(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public PushNotificationSendResult send(PushNotificationTarget target, PushNotificationPayload payload) {
        Message message = Message.builder()
            .setToken(target.token())
            .setNotification(Notification.builder()
                .setTitle(payload.title())
                .setBody(payload.message())
                .build())
            .putData("notificationId", payload.notificationId().toString())
            .putData("source", "sanctuary-admin")
            .build();

        try {
            return PushNotificationSendResult.sent(firebaseMessaging.send(message));
        } catch (FirebaseMessagingException exception) {
            return PushNotificationSendResult.failed(
                exception.getMessage(),
                isInvalidToken(exception)
            );
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException exception) {
        if (exception.getMessagingErrorCode() == null) {
            return false;
        }

        return switch (exception.getMessagingErrorCode()) {
            case UNREGISTERED, INVALID_ARGUMENT, SENDER_ID_MISMATCH -> true;
            default -> false;
        };
    }
}
