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
        String clientInstanceId = emptyToNull(request.clientInstanceId());
        boolean automatedTest = Boolean.TRUE.equals(request.automatedTest());
        String checkInSource = normalizeCheckInSource(request.checkInSource(), clientInstanceId, automatedTest, false);
        if (anonymousDeviceId != null) {
            upsertAnonymousDevice(
                anonymousDeviceId,
                userId,
                request.platform(),
                request.appVersion(),
                request.language(),
                request.timeZoneId(),
                null,
                null,
                clientInstanceId,
                automatedTest,
                checkInSource
            );
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
                    time_zone_id,
                    client_instance_id,
                    automated_test,
                    check_in_source
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            userId,
            anonymousDeviceId,
            request.eventType(),
            request.platform(),
            emptyToNull(request.appVersion()),
            request.language(),
            emptyToNull(request.timeZoneId()),
            clientInstanceId,
            automatedTest,
            checkInSource
        );
    }

    public void recordAnonymous(AnonymousAppActivityRequest request) {
        String clientInstanceId = emptyToNull(request.clientInstanceId());
        boolean automatedTest = Boolean.TRUE.equals(request.automatedTest());
        String checkInSource = normalizeCheckInSource(request.checkInSource(), clientInstanceId, automatedTest, true);
        upsertAnonymousDevice(
            request.anonymousDeviceId(),
            null,
            request.platform(),
            request.appVersion(),
            request.language(),
            request.timeZoneId(),
            request.fcmToken(),
            request.notificationsEnabled(),
            clientInstanceId,
            automatedTest,
            checkInSource
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
                    screen_name,
                    client_instance_id,
                    automated_test,
                    check_in_source
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            request.anonymousDeviceId(),
            request.eventType(),
            request.platform(),
            emptyToNull(request.appVersion()),
            request.language(),
            emptyToNull(request.timeZoneId()),
            emptyToNull(request.screenName()),
            clientInstanceId,
            automatedTest,
            checkInSource
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
        Boolean notificationsEnabled,
        String clientInstanceId,
        boolean automatedTest,
        String checkInSource
    ) {
        String normalizedFcmToken = emptyToNull(fcmToken);
        removeDuplicateAnonymousDevicesForToken(anonymousDeviceId, normalizedFcmToken);

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
                    token_status,
                    client_instance_id,
                    automated_test,
                    check_in_source
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, FALSE), 'valid', ?, ?, ?)
                ON CONFLICT (anonymous_device_id) DO UPDATE
                SET
                    linked_user_id = EXCLUDED.linked_user_id,
                    platform = EXCLUDED.platform,
                    app_version = EXCLUDED.app_version,
                    language = EXCLUDED.language,
                    time_zone_id = EXCLUDED.time_zone_id,
                    fcm_token = COALESCE(EXCLUDED.fcm_token, anonymous_app_devices.fcm_token),
                    notifications_enabled = COALESCE(?, anonymous_app_devices.notifications_enabled),
                    client_instance_id = COALESCE(EXCLUDED.client_instance_id, anonymous_app_devices.client_instance_id),
                    automated_test = EXCLUDED.automated_test,
                    check_in_source = EXCLUDED.check_in_source,
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
            normalizedFcmToken,
            notificationsEnabled,
            clientInstanceId,
            automatedTest,
            checkInSource,
            notificationsEnabled
        );
    }

    private void removeDuplicateAnonymousDevicesForToken(String anonymousDeviceId, String fcmToken) {
        if (fcmToken == null) {
            return;
        }

        jdbcTemplate.update(
            """
                DELETE FROM anonymous_app_devices
                WHERE anonymous_device_id <> ?
                  AND fcm_token = ?
                """,
            anonymousDeviceId,
            fcmToken
        );
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeCheckInSource(String requestedSource, String clientInstanceId, boolean automatedTest, boolean anonymous) {
        if (automatedTest) {
            return "automated_test";
        }

        String source = emptyToNull(requestedSource);
        if ("automated_test".equals(source)) {
            return "automated_test";
        }
        if ("app".equals(source)) {
            return "app";
        }

        return anonymous && clientInstanceId == null ? "legacy" : "app";
    }
}
