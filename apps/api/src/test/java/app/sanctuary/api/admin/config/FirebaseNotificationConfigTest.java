package app.sanctuary.api.admin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import app.sanctuary.api.admin.notification.DisabledPushNotificationGateway;
import app.sanctuary.api.admin.notification.FirebasePushNotificationGateway;
import app.sanctuary.api.admin.notification.PushNotificationGateway;
import app.sanctuary.api.admin.repository.AdminAuditRepository;
import app.sanctuary.api.admin.repository.AdminNotificationRepository;
import app.sanctuary.api.admin.service.AdminNotificationService;

class FirebaseNotificationConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(FirebaseNotificationConfig.class, AdminNotificationService.class)
        .withBean(AdminNotificationRepository.class, () -> mock(AdminNotificationRepository.class))
        .withBean(AdminAuditRepository.class, () -> mock(AdminAuditRepository.class));

    @Test
    void createsDisabledGatewayWhenFirebaseNotificationsAreOff() {
        contextRunner
            .withPropertyValues("sanctuary.notifications.firebase.enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(PushNotificationGateway.class);
                assertThat(context).hasSingleBean(DisabledPushNotificationGateway.class);
                assertThat(context).hasSingleBean(AdminNotificationService.class);
            });
    }

    @Test
    void failsFastWhenFirebaseNotificationsAreEnabledWithoutCredentials() {
        contextRunner
            .withPropertyValues("sanctuary.notifications.firebase.enabled=true")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseMessage("Firebase notifications are enabled, but service account JSON is missing.");
            });
    }

    @Test
    void createsFirebaseGatewayWhenFirebaseCredentialsAreConfigured() throws Exception {
        String appName = "sanctuary-api-test-" + UUID.randomUUID();

        contextRunner
            .withPropertyValues(
                "sanctuary.notifications.firebase.enabled=true",
                "sanctuary.notifications.firebase.app-name=" + appName,
                "sanctuary.notifications.firebase.service-account-json=" + fakeServiceAccountJson()
            )
            .run(context -> {
                try {
                    assertThat(context).hasSingleBean(PushNotificationGateway.class);
                    assertThat(context).hasSingleBean(FirebasePushNotificationGateway.class);
                    assertThat(context).hasSingleBean(AdminNotificationService.class);
                } finally {
                    FirebaseApp.getApps().stream()
                        .filter(app -> app.getName().equals(appName))
                        .findFirst()
                        .ifPresent(FirebaseApp::delete);
                }
            });
    }

    private static String fakeServiceAccountJson() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String privateKey = Base64.getMimeEncoder(64, "\n".getBytes())
            .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\\n"
            + privateKey.replace("\n", "\\n")
            + "\\n-----END PRIVATE KEY-----\\n";

        return """
            {"type":"service_account","project_id":"sanctuary-test","private_key_id":"test-key","private_key":"%s","client_email":"firebase-adminsdk-test@sanctuary-test.iam.gserviceaccount.com","client_id":"123456789","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token"}
            """.formatted(pem).trim();
    }
}
