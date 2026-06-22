package app.sanctuary.api.admin.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import app.sanctuary.api.admin.dto.AdminDeviceInstallDto;
import app.sanctuary.api.admin.dto.AdminUserListItemDto;
import app.sanctuary.api.admin.dto.AdminUserMetricsDto;

@ExtendWith(MockitoExtension.class)
class AdminUserRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    @Test
    void metricsMapsProductionVisibilityCounts() throws Exception {
        AdminUserRepository repository = new AdminUserRepository(jdbcTemplate);
        ArgumentCaptor<RowMapper<AdminUserMetricsDto>> mapperCaptor = ArgumentCaptor.captor();
        when(jdbcTemplate.queryForObject(anyString(), mapperCaptor.capture())).thenReturn(null);

        repository.metrics();

        when(resultSet.getInt("total_users")).thenReturn(12);
        when(resultSet.getInt("registered_users_today")).thenReturn(1);
        when(resultSet.getInt("active_users_today")).thenReturn(2);
        when(resultSet.getInt("active_users_7_days")).thenReturn(5);
        when(resultSet.getInt("active_users_30_days")).thenReturn(8);
        when(resultSet.getInt("anonymous_device_count")).thenReturn(21);
        when(resultSet.getInt("anonymous_linked_device_count")).thenReturn(4);
        when(resultSet.getInt("anonymous_active_devices_today")).thenReturn(6);
        when(resultSet.getInt("anonymous_active_devices_7_days")).thenReturn(14);
        when(resultSet.getInt("anonymous_active_devices_30_days")).thenReturn(19);
        when(resultSet.getInt("anonymous_app_open_events_today")).thenReturn(31);
        when(resultSet.getInt("anonymous_app_open_events_7_days")).thenReturn(99);
        when(resultSet.getInt("anonymous_session_start_events_today")).thenReturn(30);
        when(resultSet.getInt("anonymous_notification_permission_allowed_count")).thenReturn(11);
        when(resultSet.getInt("anonymous_notification_permission_denied_count")).thenReturn(3);
        when(resultSet.getInt("anonymous_ios_device_count")).thenReturn(13);
        when(resultSet.getInt("anonymous_android_device_count")).thenReturn(8);
        when(resultSet.getInt("active_known_device_count_recent")).thenReturn(10);
        when(resultSet.getInt("active_known_device_count_30_days")).thenReturn(18);
        when(resultSet.getInt("push_ready_ios_device_count")).thenReturn(6);
        when(resultSet.getInt("push_ready_android_device_count")).thenReturn(4);
        when(resultSet.getInt("active_app_users_today")).thenReturn(3);
        when(resultSet.getInt("active_app_users_7_days")).thenReturn(7);
        when(resultSet.getInt("active_app_users_30_days")).thenReturn(9);
        when(resultSet.getInt("app_open_events_today")).thenReturn(11);
        when(resultSet.getInt("app_open_events_7_days")).thenReturn(22);
        when(resultSet.getInt("session_start_events_today")).thenReturn(10);
        when(resultSet.getInt("device_count")).thenReturn(13);
        when(resultSet.getInt("active_devices_recent")).thenReturn(4);
        when(resultSet.getInt("active_devices_7_days")).thenReturn(6);
        when(resultSet.getInt("active_devices_30_days")).thenReturn(9);
        when(resultSet.getInt("ios_device_count")).thenReturn(7);
        when(resultSet.getInt("android_device_count")).thenReturn(6);
        when(resultSet.getInt("english_device_count")).thenReturn(8);
        when(resultSet.getInt("spanish_device_count")).thenReturn(3);
        when(resultSet.getInt("polish_device_count")).thenReturn(2);
        when(resultSet.getInt("notifications_enabled_device_count")).thenReturn(10);
        when(resultSet.getInt("valid_token_count")).thenReturn(12);
        when(resultSet.getInt("invalid_token_count")).thenReturn(1);
        when(resultSet.getInt("unknown_app_version_device_count")).thenReturn(4);
        when(resultSet.getInt("notification_targeted_count")).thenReturn(25);
        when(resultSet.getInt("notification_sent_count")).thenReturn(20);
        when(resultSet.getInt("notification_failed_count")).thenReturn(5);

        AdminUserMetricsDto metrics = mapperCaptor.getValue().mapRow(resultSet, 0);

        assertEquals(12, metrics.totalUsers());
        assertEquals(1, metrics.registeredUsersToday());
        assertEquals(2, metrics.activeUsersToday());
        assertEquals(5, metrics.activeUsers7Days());
        assertEquals(8, metrics.activeUsers30Days());
        assertEquals(21, metrics.anonymousDeviceCount());
        assertEquals(4, metrics.anonymousLinkedDeviceCount());
        assertEquals(6, metrics.anonymousActiveDevicesToday());
        assertEquals(14, metrics.anonymousActiveDevices7Days());
        assertEquals(19, metrics.anonymousActiveDevices30Days());
        assertEquals(31, metrics.anonymousAppOpenEventsToday());
        assertEquals(99, metrics.anonymousAppOpenEvents7Days());
        assertEquals(30, metrics.anonymousSessionStartEventsToday());
        assertEquals(11, metrics.anonymousNotificationPermissionAllowedCount());
        assertEquals(3, metrics.anonymousNotificationPermissionDeniedCount());
        assertEquals(13, metrics.anonymousIosDeviceCount());
        assertEquals(8, metrics.anonymousAndroidDeviceCount());
        assertEquals(10, metrics.activeKnownDeviceCountRecent());
        assertEquals(18, metrics.activeKnownDeviceCount30Days());
        assertEquals(6, metrics.pushReadyIosDeviceCount());
        assertEquals(4, metrics.pushReadyAndroidDeviceCount());
        assertEquals(3, metrics.activeAppUsersToday());
        assertEquals(7, metrics.activeAppUsers7Days());
        assertEquals(9, metrics.activeAppUsers30Days());
        assertEquals(11, metrics.appOpenEventsToday());
        assertEquals(22, metrics.appOpenEvents7Days());
        assertEquals(10, metrics.sessionStartEventsToday());
        assertEquals(13, metrics.deviceCount());
        assertEquals(4, metrics.activeDevicesRecent());
        assertEquals(6, metrics.activeDevices7Days());
        assertEquals(9, metrics.activeDevices30Days());
        assertEquals(7, metrics.iosDeviceCount());
        assertEquals(6, metrics.androidDeviceCount());
        assertEquals(8, metrics.englishDeviceCount());
        assertEquals(3, metrics.spanishDeviceCount());
        assertEquals(2, metrics.polishDeviceCount());
        assertEquals(10, metrics.notificationsEnabledDeviceCount());
        assertEquals(12, metrics.validTokenCount());
        assertEquals(1, metrics.invalidTokenCount());
        assertEquals(4, metrics.unknownAppVersionDeviceCount());
        assertEquals(25, metrics.notificationTargetedCount());
        assertEquals(20, metrics.notificationSentCount());
        assertEquals(5, metrics.notificationFailedCount());
    }

    @Test
    void listUsersMapsLatestDeviceActivity() throws Exception {
        AdminUserRepository repository = new AdminUserRepository(jdbcTemplate);
        ArgumentCaptor<RowMapper<AdminUserListItemDto>> mapperCaptor = ArgumentCaptor.captor();
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), eq(25))).thenReturn(List.of());

        repository.listUsers(25);

        UUID userId = UUID.randomUUID();
        OffsetDateTime registrationDate = OffsetDateTime.now().minusDays(30);
        OffsetDateTime lastSignInAt = OffsetDateTime.now().minusDays(1);
        OffsetDateTime latestDeviceLastSeenAt = OffsetDateTime.now().minusHours(2);
        when(resultSet.getObject("user_id", UUID.class)).thenReturn(userId);
        when(resultSet.getString("email")).thenReturn("admin@example.com");
        when(resultSet.getString("display_name")).thenReturn("Admin User");
        when(resultSet.getString("preferred_language")).thenReturn("en");
        when(resultSet.getObject("registration_date", OffsetDateTime.class)).thenReturn(registrationDate);
        when(resultSet.getObject("last_sign_in_at", OffsetDateTime.class)).thenReturn(lastSignInAt);
        when(resultSet.getInt("device_count")).thenReturn(1);
        when(resultSet.getString("latest_platform")).thenReturn("ios");
        when(resultSet.getString("latest_app_version")).thenReturn("1.0.12");
        when(resultSet.getString("latest_device_language")).thenReturn("es");
        when(resultSet.getObject("latest_device_last_seen_at", OffsetDateTime.class)).thenReturn(latestDeviceLastSeenAt);
        when(resultSet.getBoolean("notifications_enabled")).thenReturn(true);
        when(resultSet.getBoolean("admin")).thenReturn(true);

        AdminUserListItemDto user = mapperCaptor.getValue().mapRow(resultSet, 0);

        assertEquals(userId, user.userId());
        assertEquals("admin@example.com", user.email());
        assertEquals("Admin User", user.displayName());
        assertEquals("en", user.preferredLanguage());
        assertEquals(registrationDate, user.registrationDate());
        assertEquals(lastSignInAt, user.lastSignInAt());
        assertEquals(1, user.deviceCount());
        assertEquals("ios", user.latestPlatform());
        assertEquals("1.0.12", user.latestAppVersion());
        assertEquals("es", user.latestDeviceLanguage());
        assertEquals(latestDeviceLastSeenAt, user.latestDeviceLastSeenAt());
        assertEquals(true, user.notificationsEnabled());
        assertEquals(true, user.admin());
    }

    @Test
    void listRecentDeviceInstallsMapsAnonymousDeviceMetadata() throws Exception {
        AdminUserRepository repository = new AdminUserRepository(jdbcTemplate);
        ArgumentCaptor<RowMapper<AdminDeviceInstallDto>> mapperCaptor = ArgumentCaptor.captor();
        when(jdbcTemplate.query(anyString(), mapperCaptor.capture(), eq(25))).thenReturn(List.of());

        repository.listRecentDeviceInstalls(25);

        OffsetDateTime firstSeenAt = OffsetDateTime.now().minusHours(1);
        OffsetDateTime lastSeenAt = OffsetDateTime.now();
        when(resultSet.getString("id")).thenReturn("android-device-1");
        when(resultSet.getObject("user_id", UUID.class)).thenReturn(null);
        when(resultSet.getString("user_email")).thenReturn(null);
        when(resultSet.getString("user_display_name")).thenReturn(null);
        when(resultSet.getBoolean("signed_in")).thenReturn(false);
        when(resultSet.getString("platform")).thenReturn("android");
        when(resultSet.getString("app_version")).thenReturn("1.0.12-dev");
        when(resultSet.getString("language")).thenReturn("en");
        when(resultSet.getBoolean("notifications_enabled")).thenReturn(true);
        when(resultSet.getString("token_status")).thenReturn("valid");
        when(resultSet.getBoolean("push_ready")).thenReturn(true);
        when(resultSet.getObject("first_seen_at", OffsetDateTime.class)).thenReturn(firstSeenAt);
        when(resultSet.getObject("last_seen_at", OffsetDateTime.class)).thenReturn(lastSeenAt);

        AdminDeviceInstallDto install = mapperCaptor.getValue().mapRow(resultSet, 0);

        assertEquals("android-device-1", install.id());
        assertEquals(null, install.userId());
        assertEquals(null, install.userEmail());
        assertEquals(null, install.userDisplayName());
        assertEquals(false, install.signedIn());
        assertEquals("android", install.platform());
        assertEquals("1.0.12-dev", install.appVersion());
        assertEquals("en", install.language());
        assertEquals(true, install.notificationsEnabled());
        assertEquals("valid", install.tokenStatus());
        assertEquals(true, install.pushReady());
        assertEquals(firstSeenAt, install.firstSeenAt());
        assertEquals(lastSeenAt, install.lastSeenAt());
    }
}
