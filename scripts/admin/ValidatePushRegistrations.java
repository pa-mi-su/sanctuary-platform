import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import java.io.ByteArrayInputStream;
import java.util.*;

/** Validation only. Never prints registration tokens, credentials or provider error messages. */
class ValidatePushRegistrations {
    public static void main(String[] args) throws Exception {
        var json = new ObjectMapper();
        var input = json.readTree(System.in);
        var credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json.writeValueAsBytes(input.get("credentials"))));
        var app = FirebaseApp.initializeApp(FirebaseOptions.builder().setProjectId("sanctuary-7f082").setCredentials(credentials).build(), "registration-audit");
        var messaging = FirebaseMessaging.getInstance(app);
        var results = new ArrayList<Map<String,Object>>();
        try {
            for (var device : input.get("devices")) {
                String status = "APNS_NOT_VERIFIED";
                if ("android".equals(device.get("platform").asText())) {
                    try {
                        // true is mandatory: Firebase validates without delivering any message.
                        messaging.send(Message.builder().setToken(device.get("token").asText())
                            .putData("sanctuary_registration_audit", "validation_only").build(), true);
                        status = "FCM_VALIDATED";
                    } catch (FirebaseMessagingException e) {
                        status = e.getMessagingErrorCode() == null ? "PROVIDER_ERROR" : e.getMessagingErrorCode().name();
                    }
                }
                results.add(Map.of("id",device.get("id").asText(),"platform",device.get("platform").asText(),"status",status));
            }
            System.out.println(json.writeValueAsString(results));
        } finally { app.delete(); }
    }
}
