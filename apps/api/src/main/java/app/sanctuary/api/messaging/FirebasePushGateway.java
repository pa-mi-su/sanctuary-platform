package app.sanctuary.api.messaging;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import com.google.firebase.messaging.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FirebasePushGateway implements PushGateway {
    private final FirebaseMessaging messaging;
    public FirebasePushGateway(@Value("${sanctuary.push.enabled:false}") boolean enabled,
        @Value("${sanctuary.push.credentials:}") String credentials) throws java.io.IOException {
        if (!enabled) { messaging = null; return; }
        if (credentials.isBlank()) throw new IllegalStateException("Push messaging credentials are missing");
        var account = GoogleCredentials.fromStream(new ByteArrayInputStream(credentials.getBytes(StandardCharsets.UTF_8)));
        var app = FirebaseApp.initializeApp(FirebaseOptions.builder().setCredentials(account).setProjectId("sanctuary-7f082").build(), "sanctuary-push");
        messaging = FirebaseMessaging.getInstance(app);
    }
    public boolean enabled() { return messaging != null; }
    public Result send(String token, String title, String body, String destination, String messageId) {
        if (!enabled()) return new Result(false,false,"DISABLED",null);
        var message = Message.builder().setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putData("destination", destination).putData("messageId", messageId)
            .setAndroidConfig(AndroidConfig.builder().setTtl(86400000L)
                .setNotification(AndroidNotification.builder().setChannelId("sanctuary-announcements").build()).build())
            .setApnsConfig(ApnsConfig.builder().putHeader("apns-expiration", Long.toString(java.time.Instant.now().plusSeconds(86400).getEpochSecond()))
                .setAps(Aps.builder().setSound("default").build()).build()).build();
        try { return new Result(true,false,null,messaging.send(message)); }
        catch (FirebaseMessagingException e) {
            var code = e.getMessagingErrorCode();
            return new Result(false,code == MessagingErrorCode.UNREGISTERED,code == null ? "PROVIDER_ERROR" : code.name(),null);
        }
    }
}
