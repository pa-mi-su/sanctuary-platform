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
        when(resultSet.getInt("active_users_today")).thenReturn(2);
        when(resultSet.getInt("active_users_7_days")).thenReturn(5);
        when(resultSet.getInt("active_users_30_days")).thenReturn(8);
        when(resultSet.getInt("device_count")).thenReturn(13);
        when(resultSet.getInt("active_devices_7_days")).thenReturn(6);
        when(resultSet.getInt("active_devices_30_days")).thenReturn(9);
        when(resultSet.getInt("ios_device_count")).thenReturn(7);
        when(resultSet.getInt("android_device_count")).thenReturn(6);
        when(resultSet.getInt("english_device_count")).thenReturn(8);
        when(resultSet.getInt("spanish_device_count")).thenReturn(3);
        when(resultSet.getInt("polish_device_count")).thenReturn(2);
        when(resultSet.getInt("notifications_enabled_device_count")).thenReturn(10);
        when(resultSet.getInt("invalid_token_count")).thenReturn(1);
        when(resultSet.getInt("unknown_app_version_device_count")).thenReturn(4);

        AdminUserMetricsDto metrics = mapperCaptor.getValue().mapRow(resultSet, 0);

        assertEquals(12, metrics.totalUsers());
        assertEquals(2, metrics.activeUsersToday());
        assertEquals(5, metrics.activeUsers7Days());
        assertEquals(8, metrics.activeUsers30Days());
        assertEquals(13, metrics.deviceCount());
        assertEquals(6, metrics.activeDevices7Days());
        assertEquals(9, metrics.activeDevices30Days());
        assertEquals(7, metrics.iosDeviceCount());
        assertEquals(6, metrics.androidDeviceCount());
        assertEquals(8, metrics.englishDeviceCount());
        assertEquals(3, metrics.spanishDeviceCount());
        assertEquals(2, metrics.polishDeviceCount());
        assertEquals(10, metrics.notificationsEnabledDeviceCount());
        assertEquals(1, metrics.invalidTokenCount());
        assertEquals(4, metrics.unknownAppVersionDeviceCount());
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
        when(resultSet.getInt("device_count")).thenReturn(2);
        when(resultSet.getString("latest_platform")).thenReturn("ios");
        when(resultSet.getString("latest_app_version")).thenReturn("1.0.12");
        when(resultSet.getString("latest_device_language")).thenReturn("es");
        when(resultSet.getObject("latest_device_last_seen_at", OffsetDateTime.class)).thenReturn(latestDeviceLastSeenAt);
        when(resultSet.getBoolean("notifications_enabled")).thenReturn(true);

        AdminUserListItemDto user = mapperCaptor.getValue().mapRow(resultSet, 0);

        assertEquals(userId, user.userId());
        assertEquals("admin@example.com", user.email());
        assertEquals("Admin User", user.displayName());
        assertEquals("en", user.preferredLanguage());
        assertEquals(registrationDate, user.registrationDate());
        assertEquals(lastSignInAt, user.lastSignInAt());
        assertEquals(2, user.deviceCount());
        assertEquals("ios", user.latestPlatform());
        assertEquals("1.0.12", user.latestAppVersion());
        assertEquals("es", user.latestDeviceLanguage());
        assertEquals(latestDeviceLastSeenAt, user.latestDeviceLastSeenAt());
        assertEquals(true, user.notificationsEnabled());
    }
}
