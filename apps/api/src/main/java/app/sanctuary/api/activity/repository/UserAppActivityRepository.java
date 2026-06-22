package app.sanctuary.api.activity.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.activity.dto.AnonymousAppActivityRequest;
import app.sanctuary.api.activity.dto.UserAppActivityRequest;

@Repository
public class UserAppActivityRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserAppActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(UUID userId, UserAppActivityRequest request) {
        String anonymousDeviceId = emptyToNull(request.anonymousDeviceId());
        if (anonymousDeviceId != null) {
            upsertAnonymousDevice(anonymousDeviceId, userId, request.platform(), request.appVersion(), request.language(), request.timeZoneId(), null, null);
        }
        jdbcTemplate.update(
            """
                INSERT INTO user_app_activity_events (
                    user_id,
                    anonymous_device_id,
                    event_type,
                    platform,
                    app_version,
                    language,
                    time_zone_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            userId,
            anonymousDeviceId,
            request.eventType(),
            request.platform(),
            emptyToNull(request.appVersion()),
            request.language(),
            emptyToNull(request.timeZoneId())
        );
    }

    public void recordAnonymous(AnonymousAppActivityRequest request) {
        upsertAnonymousDevice(
            request.anonymousDeviceId(),
            null,
            request.platform(),
            request.appVersion(),
            request.language(),
            request.timeZoneId(),
            request.fcmToken(),
            request.notificationsEnabled()
        );

        jdbcTemplate.update(
            """
                INSERT INTO anonymous_app_activity_events (
                    anonymous_device_id,
                    event_type,
                    platform,
                    app_version,
                    language,
                    time_zone_id,
                    screen_name
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            request.anonymousDeviceId(),
            request.eventType(),
            request.platform(),
            emptyToNull(request.appVersion()),
            request.language(),
            emptyToNull(request.timeZoneId()),
            emptyToNull(request.screenName())
        );
    }

    private void upsertAnonymousDevice(
        String anonymousDeviceId,
        UUID userId,
        String platform,
        String appVersion,
        String language,
        String timeZoneId,
        String fcmToken,
        Boolean notificationsEnabled
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO anonymous_app_devices (
                    anonymous_device_id,
                    linked_user_id,
                    platform,
                    app_version,
                    language,
                    time_zone_id,
                    fcm_token,
                    notifications_enabled,
                    token_status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, FALSE), 'valid')
                ON CONFLICT (anonymous_device_id) DO UPDATE
                SET
                    linked_user_id = COALESCE(EXCLUDED.linked_user_id, anonymous_app_devices.linked_user_id),
                    platform = EXCLUDED.platform,
                    app_version = EXCLUDED.app_version,
                    language = EXCLUDED.language,
                    time_zone_id = EXCLUDED.time_zone_id,
                    fcm_token = COALESCE(EXCLUDED.fcm_token, anonymous_app_devices.fcm_token),
                    notifications_enabled = COALESCE(?, anonymous_app_devices.notifications_enabled),
                    token_status = CASE
                        WHEN EXCLUDED.fcm_token IS NOT NULL THEN 'valid'
                        ELSE anonymous_app_devices.token_status
                    END,
                    last_seen_at = NOW(),
                    updated_at = NOW()
                """,
            anonymousDeviceId,
            userId,
            platform,
            emptyToNull(appVersion),
            language,
            emptyToNull(timeZoneId),
            emptyToNull(fcmToken),
            notificationsEnabled,
            notificationsEnabled
        );
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
