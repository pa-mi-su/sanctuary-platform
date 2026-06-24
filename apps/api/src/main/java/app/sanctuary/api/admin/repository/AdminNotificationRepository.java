package app.sanctuary.api.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.admin.dto.AdminNotificationDeliveryDto;
import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;
import app.sanctuary.api.admin.notification.PushNotificationTarget;

@Repository
public class AdminNotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminNotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminNotificationDto createDraft(UUID createdByUserId, AdminNotificationRequest request) {
        return jdbcTemplate.queryForObject(
            """
                INSERT INTO admin_notifications (
                    title,
                    message,
                    audience_type,
                    status,
                    created_by_user_id,
                    updated_at
                )
                VALUES (?, ?, 'all', 'draft', ?, NOW())
                RETURNING
                    id,
                    title,
                    message,
                    audience_type,
                    status,
                    target_count,
                    sent_count,
                    failed_count,
                    sent_at,
                    created_at,
                    updated_at
                """,
            this::mapNotification,
            request.title().trim(),
            request.message().trim(),
            createdByUserId
        );
    }

    public List<AdminNotificationDto> history(int limit) {
        return jdbcTemplate.query(
            """
                SELECT
                    id,
                    title,
                    message,
                    audience_type,
                    status,
                    target_count,
                    sent_count,
                    failed_count,
                    sent_at,
                    created_at,
                    updated_at
                FROM admin_notifications
                ORDER BY created_at DESC
                LIMIT ?
                """,
            this::mapNotification,
            limit
        );
    }

    public AdminNotificationDto findById(UUID notificationId) {
        return jdbcTemplate.query(
            """
                SELECT
                    id,
                    title,
                    message,
                    audience_type,
                    status,
                    target_count,
                    sent_count,
                    failed_count,
                    sent_at,
                    created_at,
                    updated_at
                FROM admin_notifications
                WHERE id = ?
                """,
            this::mapNotification,
            notificationId
        ).stream().findFirst().orElse(null);
    }

    public List<AdminNotificationDeliveryDto> recentDeliveries(int limit) {
        return jdbcTemplate.query(
            """
                SELECT
                    d.id,
                    d.notification_id,
                    n.title AS notification_title,
                    d.user_device_id,
                    d.anonymous_device_id,
                    d.user_id,
                    d.platform,
                    d.status,
                    d.failure_reason,
                    d.sent_at,
                    d.created_at,
                    d.updated_at
                FROM admin_notification_deliveries d
                JOIN admin_notifications n ON n.id = d.notification_id
                ORDER BY d.created_at DESC
                LIMIT ?
                """,
            (rs, rowNum) -> new AdminNotificationDeliveryDto(
                rs.getObject("id", UUID.class),
                rs.getObject("notification_id", UUID.class),
                rs.getString("notification_title"),
                rs.getObject("user_device_id", UUID.class),
                rs.getString("anonymous_device_id"),
                rs.getObject("user_id", UUID.class),
                rs.getString("platform"),
                rs.getString("status"),
                rs.getString("failure_reason"),
                rs.getObject("sent_at", java.time.OffsetDateTime.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            limit
        );
    }

    public boolean markSending(UUID notificationId, UUID sentByUserId, int targetCount) {
        int updated = jdbcTemplate.update(
            """
                UPDATE admin_notifications
                SET
                    status = 'sending',
                    sent_by_user_id = ?,
                    target_count = ?,
                    sent_count = 0,
                    failed_count = 0,
                    updated_at = NOW()
                WHERE id = ?
                  AND status = 'draft'
                """,
            sentByUserId,
            targetCount,
            notificationId
        );
        return updated == 1;
    }

    public void finishSend(UUID notificationId, String status, int sentCount, int failedCount) {
        jdbcTemplate.update(
            """
                UPDATE admin_notifications
                SET
                    status = ?,
                    sent_count = ?,
                    failed_count = ?,
                    sent_at = NOW(),
                    updated_at = NOW()
                WHERE id = ?
                """,
            status,
            sentCount,
            failedCount,
            notificationId
        );
    }

    public List<PushNotificationTarget> findValidTargetsForAllAudience() {
        return jdbcTemplate.query(
            """
                WITH targets AS (
                    SELECT
                        id AS user_device_id,
                        NULL::text AS anonymous_device_id,
                        user_id,
                        platform,
                        fcm_token,
                        updated_at,
                        0 AS priority
                    FROM user_devices
                    WHERE notifications_enabled = TRUE
                      AND token_status = 'valid'
                      AND automated_test = FALSE
                      AND check_in_source = 'app'
                      AND NULLIF(TRIM(client_instance_id), '') IS NOT NULL
                      AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                      AND EXISTS (
                          SELECT 1
                          FROM user_app_activity_events e
                          WHERE e.user_id = user_devices.user_id
                            AND e.event_type = 'foreground_heartbeat'
                            AND e.occurred_at >= NOW() - INTERVAL '2 minutes'
                            AND e.automated_test = FALSE
                            AND e.check_in_source = 'app'
                            AND NULLIF(TRIM(e.client_instance_id), '') = NULLIF(TRIM(user_devices.client_instance_id), '')
                      )

                    UNION ALL

                    SELECT
                        NULL::uuid AS user_device_id,
                        anonymous_device_id,
                        linked_user_id AS user_id,
                        platform,
                        fcm_token,
                        updated_at,
                        1 AS priority
                    FROM anonymous_app_devices
                    WHERE notifications_enabled = TRUE
                      AND token_status = 'valid'
                      AND linked_user_id IS NULL
                      AND automated_test = FALSE
                      AND check_in_source = 'app'
                      AND NULLIF(TRIM(client_instance_id), '') IS NOT NULL
                      AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                      AND EXISTS (
                          SELECT 1
                          FROM anonymous_app_activity_events e
                          WHERE e.anonymous_device_id = anonymous_app_devices.anonymous_device_id
                            AND e.event_type = 'foreground_heartbeat'
                            AND e.occurred_at >= NOW() - INTERVAL '2 minutes'
                            AND e.automated_test = FALSE
                            AND e.check_in_source = 'app'
                            AND NULLIF(TRIM(e.client_instance_id), '') = NULLIF(TRIM(anonymous_app_devices.client_instance_id), '')
                      )
                ),
                ranked AS (
                    SELECT
                        *,
                        ROW_NUMBER() OVER (
                            PARTITION BY fcm_token
                            ORDER BY priority ASC, updated_at DESC
                        ) AS token_rank
                    FROM targets
                )
                SELECT
                    user_device_id,
                    anonymous_device_id,
                    user_id,
                    platform,
                    fcm_token
                FROM ranked
                WHERE token_rank = 1
                ORDER BY updated_at DESC
                """,
            (rs, rowNum) -> new PushNotificationTarget(
                rs.getObject("user_device_id", UUID.class),
                rs.getString("anonymous_device_id"),
                rs.getObject("user_id", UUID.class),
                rs.getString("platform"),
                rs.getString("fcm_token")
            )
        );
    }

    public UUID createDelivery(UUID notificationId, PushNotificationTarget target) {
        return jdbcTemplate.queryForObject(
            """
                INSERT INTO admin_notification_deliveries (
                    notification_id,
                    user_device_id,
                    anonymous_device_id,
                    user_id,
                    platform,
                    status
                )
                VALUES (?, ?, ?, ?, ?, 'targeted')
                RETURNING id
                """,
            UUID.class,
            notificationId,
            target.deviceId(),
            target.anonymousDeviceId(),
            target.userId(),
            target.platform()
        );
    }

    public void markDeliverySent(UUID deliveryId) {
        jdbcTemplate.update(
            """
                UPDATE admin_notification_deliveries
                SET
                    status = 'sent',
                    sent_at = NOW(),
                    updated_at = NOW()
                WHERE id = ?
                """,
            deliveryId
        );
    }

    public void markDeliveryFailed(UUID deliveryId, String failureReason) {
        jdbcTemplate.update(
            """
                UPDATE admin_notification_deliveries
                SET
                    status = 'failed',
                    failure_reason = ?,
                    updated_at = NOW()
                WHERE id = ?
                """,
            truncate(failureReason, 1000),
            deliveryId
        );
    }

    public void markDeviceInvalid(UUID deviceId) {
        if (deviceId == null) {
            return;
        }
        jdbcTemplate.update(
            """
                UPDATE user_devices
                SET
                    token_status = 'invalid',
                    updated_at = NOW()
                WHERE id = ?
                """,
            deviceId
        );
    }

    public void markAnonymousDeviceInvalid(String anonymousDeviceId) {
        if (anonymousDeviceId == null || anonymousDeviceId.isBlank()) {
            return;
        }
        jdbcTemplate.update(
            """
                UPDATE anonymous_app_devices
                SET
                    token_status = 'invalid',
                    updated_at = NOW()
                WHERE anonymous_device_id = ?
                """,
            anonymousDeviceId
        );
    }

    private AdminNotificationDto mapNotification(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AdminNotificationDto(
            rs.getObject("id", UUID.class),
            rs.getString("title"),
            rs.getString("message"),
            rs.getString("audience_type"),
            rs.getString("status"),
            rs.getInt("target_count"),
            rs.getInt("sent_count"),
            rs.getInt("failed_count"),
            rs.getObject("sent_at", java.time.OffsetDateTime.class),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
