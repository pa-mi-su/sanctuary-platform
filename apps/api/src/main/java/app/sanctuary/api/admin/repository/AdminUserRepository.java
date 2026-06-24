package app.sanctuary.api.admin.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.admin.dto.AdminDeviceInstallDto;
import app.sanctuary.api.admin.dto.AdminUserAccessDto;
import app.sanctuary.api.admin.dto.AdminUserListItemDto;
import app.sanctuary.api.admin.dto.AdminUserMetricsDto;

@Repository
public class AdminUserRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AdminUserMetricsDto metrics() {
        return jdbcTemplate.queryForObject(
            """
                WITH live_user_devices AS (
                    SELECT DISTINCT ON (
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.id::text)
                    )
                        d.*
                    FROM user_devices d
                    WHERE d.automated_test = FALSE
                      AND d.check_in_source = 'app'
                      AND NULLIF(TRIM(d.client_instance_id), '') IS NOT NULL
                      AND EXISTS (
                          SELECT 1
                          FROM user_app_activity_events e
                          WHERE e.user_id = d.user_id
                            AND e.event_type = 'foreground_heartbeat'
                            AND e.occurred_at >= NOW() - INTERVAL '2 minutes'
                            AND e.automated_test = FALSE
                            AND e.check_in_source = 'app'
                            AND NULLIF(TRIM(e.client_instance_id), '') = NULLIF(TRIM(d.client_instance_id), '')
                      )
                    ORDER BY
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.id::text),
                        d.updated_at DESC
                ),
                live_anonymous_devices AS (
                    SELECT DISTINCT ON (
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.anonymous_device_id)
                    )
                        d.*
                    FROM anonymous_app_devices d
                    WHERE d.linked_user_id IS NULL
                      AND d.automated_test = FALSE
                      AND d.check_in_source = 'app'
                      AND NULLIF(TRIM(d.client_instance_id), '') IS NOT NULL
                      AND EXISTS (
                          SELECT 1
                          FROM anonymous_app_activity_events e
                          WHERE e.anonymous_device_id = d.anonymous_device_id
                            AND e.event_type = 'foreground_heartbeat'
                            AND e.occurred_at >= NOW() - INTERVAL '2 minutes'
                            AND e.automated_test = FALSE
                            AND e.check_in_source = 'app'
                            AND NULLIF(TRIM(e.client_instance_id), '') = NULLIF(TRIM(d.client_instance_id), '')
                      )
                    ORDER BY
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.anonymous_device_id),
                        d.updated_at DESC
                ),
                live_devices AS (
                    SELECT platform, fcm_token, notifications_enabled, token_status, app_version
                    FROM live_user_devices
                    UNION ALL
                    SELECT platform, fcm_token, notifications_enabled, token_status, app_version
                    FROM live_anonymous_devices
                ),
                known_app_installs AS (
                    SELECT DISTINCT
                        COALESCE(NULLIF(TRIM(client_instance_id), ''), NULLIF(TRIM(fcm_token), ''), id::text) AS install_key
                    FROM user_devices
                    WHERE automated_test = FALSE
                      AND check_in_source = 'app'
                      AND (
                          NULLIF(TRIM(client_instance_id), '') IS NOT NULL
                          OR NULLIF(TRIM(fcm_token), '') IS NOT NULL
                      )
                    UNION
                    SELECT DISTINCT
                        COALESCE(NULLIF(TRIM(client_instance_id), ''), NULLIF(TRIM(fcm_token), ''), anonymous_device_id) AS install_key
                    FROM anonymous_app_devices
                    WHERE automated_test = FALSE
                      AND check_in_source = 'app'
                      AND (
                          NULLIF(TRIM(client_instance_id), '') IS NOT NULL
                          OR NULLIF(TRIM(fcm_token), '') IS NOT NULL
                      )
                )
                SELECT
                    (SELECT COUNT(*) FROM users) AS total_users,
                    (SELECT COUNT(*) FROM users WHERE created_at >= NOW() - INTERVAL '1 day') AS registered_users_today,
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '1 day') AS active_users_today,
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '30 days') AS active_users_30_days,
                    (
                        SELECT COUNT(DISTINCT NULLIF(TRIM(e.client_instance_id), ''))
                        FROM anonymous_app_activity_events e
                        INNER JOIN anonymous_app_devices d ON d.anonymous_device_id = e.anonymous_device_id
                        WHERE e.event_type = 'foreground_heartbeat'
                          AND e.occurred_at >= NOW() - INTERVAL '1 day'
                          AND e.automated_test = FALSE
                          AND e.check_in_source = 'app'
                          AND d.linked_user_id IS NULL
                    ) AS anonymous_active_devices_today,
                    (
                        SELECT COUNT(DISTINCT NULLIF(TRIM(e.client_instance_id), ''))
                        FROM anonymous_app_activity_events e
                        INNER JOIN anonymous_app_devices d ON d.anonymous_device_id = e.anonymous_device_id
                        WHERE e.event_type = 'foreground_heartbeat'
                          AND e.occurred_at >= NOW() - INTERVAL '7 days'
                          AND e.automated_test = FALSE
                          AND e.check_in_source = 'app'
                          AND d.linked_user_id IS NULL
                    ) AS anonymous_active_devices_7_days,
                    (SELECT COUNT(*) FROM known_app_installs) AS known_app_install_count,
                    (SELECT COUNT(*) FROM live_devices) AS active_known_device_count_recent,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM live_devices
                        WHERE platform = 'ios'
                          AND notifications_enabled = TRUE
                          AND token_status = 'valid'
                          AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                    ) AS push_ready_ios_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM live_devices
                        WHERE platform = 'android'
                          AND notifications_enabled = TRUE
                          AND token_status = 'valid'
                          AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                    ) AS push_ready_android_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM live_devices
                        WHERE notifications_enabled = TRUE
                          AND token_status = 'valid'
                          AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                    ) AS notifications_enabled_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM live_devices
                        WHERE token_status = 'valid'
                          AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                    ) AS valid_token_count,
                    (
                        SELECT COUNT(*)
                        FROM (
                            SELECT fcm_token FROM user_devices
                            WHERE automated_test = FALSE AND check_in_source = 'app' AND token_status = 'invalid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                            UNION ALL
                            SELECT fcm_token FROM anonymous_app_devices
                            WHERE automated_test = FALSE AND check_in_source = 'app' AND token_status = 'invalid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                        ) invalid_tokens
                    ) AS invalid_token_count,
                    (SELECT COUNT(*) FROM live_devices WHERE app_version IS NULL) AS unknown_app_version_device_count,
                    (SELECT COUNT(*) FROM admin_notification_deliveries) AS notification_targeted_count,
                    (SELECT COUNT(*) FROM admin_notification_deliveries WHERE status = 'sent') AS notification_sent_count,
                    (SELECT COUNT(*) FROM admin_notification_deliveries WHERE status = 'failed') AS notification_failed_count
                """,
            (rs, rowNum) -> new AdminUserMetricsDto(
                rs.getInt("total_users"),
                rs.getInt("registered_users_today"),
                rs.getInt("active_users_today"),
                rs.getInt("active_users_30_days"),
                rs.getInt("anonymous_active_devices_today"),
                rs.getInt("anonymous_active_devices_7_days"),
                rs.getInt("known_app_install_count"),
                rs.getInt("active_known_device_count_recent"),
                rs.getInt("push_ready_ios_device_count"),
                rs.getInt("push_ready_android_device_count"),
                rs.getInt("notifications_enabled_device_count"),
                rs.getInt("valid_token_count"),
                rs.getInt("invalid_token_count"),
                rs.getInt("unknown_app_version_device_count"),
                rs.getInt("notification_targeted_count"),
                rs.getInt("notification_sent_count"),
                rs.getInt("notification_failed_count")
            )
        );
    }

    public List<AdminUserListItemDto> listUsers(int limit) {
        return jdbcTemplate.query(
            """
                WITH live_user_devices AS (
                    SELECT DISTINCT ON (
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.id::text)
                    )
                        d.*
                    FROM user_devices d
                    WHERE d.automated_test = FALSE
                      AND d.check_in_source = 'app'
                      AND NULLIF(TRIM(d.client_instance_id), '') IS NOT NULL
                      AND EXISTS (
                          SELECT 1
                          FROM user_app_activity_events e
                          WHERE e.user_id = d.user_id
                            AND e.event_type = 'foreground_heartbeat'
                            AND e.occurred_at >= NOW() - INTERVAL '2 minutes'
                            AND e.automated_test = FALSE
                            AND e.check_in_source = 'app'
                            AND NULLIF(TRIM(e.client_instance_id), '') = NULLIF(TRIM(d.client_instance_id), '')
                      )
                    ORDER BY
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.id::text),
                        d.updated_at DESC
                )
                SELECT
                    u.id AS user_id,
                    u.email,
                    u.display_name,
                    u.preferred_language,
                    u.created_at AS registration_date,
                    u.last_sign_in_at,
                    COUNT(DISTINCT d.fcm_token) FILTER (
                        WHERE d.notifications_enabled = TRUE
                          AND d.token_status = 'valid'
                          AND NULLIF(TRIM(d.fcm_token), '') IS NOT NULL
                    )::int AS device_count,
                    latest_device.platform AS latest_platform,
                    latest_device.app_version AS latest_app_version,
                    latest_device.language AS latest_device_language,
                    latest_device.last_seen_at AS latest_device_last_seen_at,
                    COALESCE(BOOL_OR(
                        d.notifications_enabled = TRUE
                        AND d.token_status = 'valid'
                        AND NULLIF(TRIM(d.fcm_token), '') IS NOT NULL
                    ), FALSE) AS notifications_enabled,
                    COALESCE(a.enabled, FALSE) AS admin
                FROM users u
                LEFT JOIN live_user_devices d ON d.user_id = u.id
                LEFT JOIN admin_users a ON a.user_id = u.id
                LEFT JOIN LATERAL (
                    SELECT platform, app_version, language, last_seen_at
                    FROM user_devices
                    WHERE user_id = u.id
                      AND automated_test = FALSE
                      AND check_in_source = 'app'
                    ORDER BY updated_at DESC
                    LIMIT 1
                ) latest_device ON TRUE
                GROUP BY
                    u.id,
                    u.email,
                    u.display_name,
                    u.preferred_language,
                    u.created_at,
                    u.last_sign_in_at,
                    latest_device.platform,
                    latest_device.app_version,
                    latest_device.language,
                    latest_device.last_seen_at,
                    a.enabled
                ORDER BY u.created_at DESC
                LIMIT ?
                """,
            (rs, rowNum) -> new AdminUserListItemDto(
                rs.getObject("user_id", UUID.class),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("preferred_language"),
                rs.getObject("registration_date", java.time.OffsetDateTime.class),
                rs.getObject("last_sign_in_at", java.time.OffsetDateTime.class),
                rs.getInt("device_count"),
                rs.getString("latest_platform"),
                rs.getString("latest_app_version"),
                rs.getString("latest_device_language"),
                rs.getObject("latest_device_last_seen_at", java.time.OffsetDateTime.class),
                rs.getBoolean("notifications_enabled"),
                rs.getBoolean("admin")
            ),
            limit
        );
    }

    public List<AdminDeviceInstallDto> listRecentDeviceInstalls(int limit) {
        return jdbcTemplate.query(
            """
                WITH live_user_devices AS (
                    SELECT DISTINCT ON (
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.id::text)
                    )
                        d.*
                    FROM user_devices d
                    WHERE d.automated_test = FALSE
                      AND d.check_in_source = 'app'
                      AND NULLIF(TRIM(d.client_instance_id), '') IS NOT NULL
                      AND EXISTS (
                          SELECT 1
                          FROM user_app_activity_events e
                          WHERE e.user_id = d.user_id
                            AND e.event_type = 'foreground_heartbeat'
                            AND e.occurred_at >= NOW() - INTERVAL '2 minutes'
                            AND e.automated_test = FALSE
                            AND e.check_in_source = 'app'
                            AND NULLIF(TRIM(e.client_instance_id), '') = NULLIF(TRIM(d.client_instance_id), '')
                      )
                    ORDER BY
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.id::text),
                        d.updated_at DESC
                ),
                live_anonymous_devices AS (
                    SELECT DISTINCT ON (
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.anonymous_device_id)
                    )
                        d.*
                    FROM anonymous_app_devices d
                    WHERE d.linked_user_id IS NULL
                      AND d.automated_test = FALSE
                      AND d.check_in_source = 'app'
                      AND NULLIF(TRIM(d.client_instance_id), '') IS NOT NULL
                      AND EXISTS (
                          SELECT 1
                          FROM anonymous_app_activity_events e
                          WHERE e.anonymous_device_id = d.anonymous_device_id
                            AND e.event_type = 'foreground_heartbeat'
                            AND e.occurred_at >= NOW() - INTERVAL '2 minutes'
                            AND e.automated_test = FALSE
                            AND e.check_in_source = 'app'
                            AND NULLIF(TRIM(e.client_instance_id), '') = NULLIF(TRIM(d.client_instance_id), '')
                      )
                    ORDER BY
                        COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.anonymous_device_id),
                        d.updated_at DESC
                )
                SELECT
                    id,
                    user_id,
                    user_email,
                    user_display_name,
                    signed_in,
                    platform,
                    app_version,
                    language,
                    notifications_enabled,
                    token_status,
                    has_push_token,
                    push_ready,
                    client_instance_id,
                    check_in_source,
                    first_seen_at,
                    last_seen_at
                FROM (
                    SELECT
                        candidates.*,
                        ROW_NUMBER() OVER (
                            PARTITION BY device_key
                            ORDER BY push_ready DESC, signed_in DESC, last_seen_at DESC
                        ) AS row_number
                    FROM (
                        SELECT
                            COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.id::text) AS device_key,
                            d.id::text AS id,
                            u.id AS user_id,
                            u.email AS user_email,
                            u.display_name AS user_display_name,
                            TRUE AS signed_in,
                            d.platform,
                            d.app_version,
                            d.language,
                            d.notifications_enabled,
                            d.token_status,
                            NULLIF(TRIM(d.fcm_token), '') IS NOT NULL AS has_push_token,
                            (
                                d.notifications_enabled = TRUE
                                AND d.token_status = 'valid'
                                AND NULLIF(TRIM(d.fcm_token), '') IS NOT NULL
                            ) AS push_ready,
                            d.client_instance_id,
                            d.check_in_source,
                            d.created_at AS first_seen_at,
                            d.last_seen_at
                        FROM live_user_devices d
                        INNER JOIN users u ON u.id = d.user_id
                        UNION ALL
                        SELECT
                            COALESCE(NULLIF(TRIM(d.fcm_token), ''), NULLIF(TRIM(d.client_instance_id), ''), d.anonymous_device_id) AS device_key,
                            d.anonymous_device_id AS id,
                            u.id AS user_id,
                            u.email AS user_email,
                            u.display_name AS user_display_name,
                            d.linked_user_id IS NOT NULL AS signed_in,
                            d.platform,
                            d.app_version,
                            d.language,
                            d.notifications_enabled,
                            d.token_status,
                            NULLIF(TRIM(d.fcm_token), '') IS NOT NULL AS has_push_token,
                            (
                                d.notifications_enabled = TRUE
                                AND d.token_status = 'valid'
                                AND NULLIF(TRIM(d.fcm_token), '') IS NOT NULL
                            ) AS push_ready,
                            d.client_instance_id,
                            d.check_in_source,
                            d.first_seen_at,
                            d.last_seen_at
                        FROM live_anonymous_devices d
                        LEFT JOIN users u ON u.id = d.linked_user_id
                    ) candidates
                ) installs
                WHERE row_number = 1
                ORDER BY last_seen_at DESC
                LIMIT ?
                """,
            (rs, rowNum) -> new AdminDeviceInstallDto(
                rs.getString("id"),
                rs.getObject("user_id", UUID.class),
                rs.getString("user_email"),
                rs.getString("user_display_name"),
                rs.getBoolean("signed_in"),
                rs.getString("platform"),
                rs.getString("app_version"),
                rs.getString("language"),
                rs.getBoolean("notifications_enabled"),
                rs.getString("token_status"),
                rs.getBoolean("has_push_token"),
                rs.getBoolean("push_ready"),
                rs.getString("client_instance_id"),
                rs.getString("check_in_source"),
                rs.getObject("first_seen_at", java.time.OffsetDateTime.class),
                rs.getObject("last_seen_at", java.time.OffsetDateTime.class)
            ),
            limit
        );
    }

    public List<AdminUserAccessDto> searchAdminAccessByEmail(String emailQuery, int limit) {
        String normalizedQuery = "%" + emailQuery.toLowerCase() + "%";
        return jdbcTemplate.query(
            """
                SELECT
                    u.id AS user_id,
                    u.email,
                    u.display_name,
                    COALESCE(a.enabled, FALSE) AS admin,
                    u.created_at AS registration_date,
                    u.last_sign_in_at
                FROM users u
                LEFT JOIN admin_users a ON a.user_id = u.id
                WHERE LOWER(COALESCE(u.email, '')) LIKE ?
                ORDER BY
                    CASE WHEN LOWER(COALESCE(u.email, '')) = LOWER(?) THEN 0 ELSE 1 END,
                    u.created_at DESC
                LIMIT ?
                """,
            this::mapAdminAccess,
            normalizedQuery,
            emailQuery,
            limit
        );
    }

    public Optional<AdminUserAccessDto> findAdminAccessByUserId(UUID userId) {
        List<AdminUserAccessDto> users = jdbcTemplate.query(
            """
                SELECT
                    u.id AS user_id,
                    u.email,
                    u.display_name,
                    COALESCE(a.enabled, FALSE) AS admin,
                    u.created_at AS registration_date,
                    u.last_sign_in_at
                FROM users u
                LEFT JOIN admin_users a ON a.user_id = u.id
                WHERE u.id = ?
                """,
            this::mapAdminAccess,
            userId
        );
        return users.stream().findFirst();
    }

    public AdminUserAccessDto setAdminAccess(UUID userId, boolean enabled) {
        jdbcTemplate.update(
            """
                INSERT INTO admin_users (user_id, enabled, notes, updated_at)
                VALUES (?, ?, 'managed from admin dashboard', NOW())
                ON CONFLICT (user_id)
                DO UPDATE SET
                    enabled = EXCLUDED.enabled,
                    notes = EXCLUDED.notes,
                    updated_at = NOW()
                """,
            userId,
            enabled
        );
        return findAdminAccessByUserId(userId).orElseThrow();
    }

    private AdminUserAccessDto mapAdminAccess(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AdminUserAccessDto(
            rs.getObject("user_id", UUID.class),
            rs.getString("email"),
            rs.getString("display_name"),
            rs.getBoolean("admin"),
            rs.getObject("registration_date", java.time.OffsetDateTime.class),
            rs.getObject("last_sign_in_at", java.time.OffsetDateTime.class)
        );
    }
}
