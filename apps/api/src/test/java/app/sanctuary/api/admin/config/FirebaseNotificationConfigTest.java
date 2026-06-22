package app.sanctuary.api.admin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import app.sanctuary.api.admin.notification.DisabledPushNotificationGateway;
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
}
