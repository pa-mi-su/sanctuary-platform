package app.sanctuary.api.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.gson.GsonFactory;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FirebasePushGatewayTest {
    @Test void visibleAnnouncementsRequestPromptDeliveryAndSoundOnBothPlatforms() throws Exception {
        long before = Instant.now().plusSeconds(86400).getEpochSecond();
        var message = FirebasePushGateway.notificationMessage("synthetic-token", "Prayer", "Take a moment to pray",
            "https://dev.mydailysanctuary.com/prayers/apostles_creed", "synthetic-message-id");
        // Use the SDK's Google JSON serializer to inspect the actual outbound payload fields.
        JsonNode json = new ObjectMapper().readTree(GsonFactory.getDefaultInstance().toString(message));
        assertEquals("high", json.at("/android/priority").asText());
        assertEquals("86400s", json.at("/android/ttl").asText());
        assertEquals("sanctuary-announcements", json.at("/android/notification/channel_id").asText());
        assertTrue(json.at("/android/notification/default_sound").asBoolean());
        assertEquals("alert", json.at("/apns/headers/apns-push-type").asText());
        assertEquals("10", json.at("/apns/headers/apns-priority").asText());
        assertEquals("default", json.at("/apns/payload/aps/sound").asText());
        long expires = json.at("/apns/headers/apns-expiration").asLong();
        assertTrue(expires >= before && expires <= Instant.now().plusSeconds(86400).getEpochSecond());
        assertEquals("Prayer", json.at("/notification/title").asText());
        assertEquals("Take a moment to pray", json.at("/notification/body").asText());
        assertEquals("https://dev.mydailysanctuary.com/prayers/apostles_creed", json.at("/data/destination").asText());
        assertEquals("synthetic-message-id", json.at("/data/messageId").asText());
    }

    @Test void disabledMessagingDoesNotNeedCredentialsOrContactFirebase() throws Exception {
        var gateway = new FirebasePushGateway(false, "");
        assertFalse(gateway.enabled());
        assertEquals("DISABLED", gateway.send("synthetic-token", "Prayer", "Message", "https://mydailysanctuary.com/", "id").code());
    }
}
