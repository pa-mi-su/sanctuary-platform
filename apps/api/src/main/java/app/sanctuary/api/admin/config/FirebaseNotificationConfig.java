package app.sanctuary.api.admin.config;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseNotificationProperties.class)
public class FirebaseNotificationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "sanctuary.notifications.firebase", name = "enabled", havingValue = "true")
    FirebaseApp sanctuaryFirebaseApp(FirebaseNotificationProperties properties) {
        String serviceAccountJson = properties.serviceAccountJson();
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            throw new IllegalStateException("Firebase notifications are enabled, but service account JSON is missing.");
        }

        try {
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))
            );
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

            return FirebaseApp.getApps().stream()
                .filter(app -> app.getName().equals(properties.resolvedAppName()))
                .findFirst()
                .orElseGet(() -> FirebaseApp.initializeApp(options, properties.resolvedAppName()));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize Firebase notifications.", exception);
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "sanctuary.notifications.firebase", name = "enabled", havingValue = "true")
    FirebaseMessaging firebaseMessaging(FirebaseApp sanctuaryFirebaseApp) {
        return FirebaseMessaging.getInstance(sanctuaryFirebaseApp);
    }
}
