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
                SELECT
                    (SELECT COUNT(*) FROM users) AS total_users,
                    (SELECT COUNT(*) FROM users WHERE created_at >= NOW() - INTERVAL '1 day') AS registered_users_today,
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '1 day') AS active_users_today,
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '30 days') AS active_users_30_days,
                    (
                        SELECT COUNT(DISTINCT COALESCE(NULLIF(TRIM(fcm_token), ''), anonymous_device_id))
                        FROM anonymous_app_devices
                        WHERE last_seen_at >= NOW() - INTERVAL '1 day'
                    ) AS anonymous_active_devices_today,
                    (
                        SELECT COUNT(DISTINCT COALESCE(NULLIF(TRIM(fcm_token), ''), anonymous_device_id))
                        FROM anonymous_app_devices
                        WHERE last_seen_at >= NOW() - INTERVAL '7 days'
                    ) AS anonymous_active_devices_7_days,
                    (
                        SELECT COUNT(DISTINCT device_key)
                        FROM (
                            SELECT COALESCE(NULLIF(TRIM(fcm_token), ''), id::text) AS device_key FROM user_devices
                            WHERE last_seen_at >= NOW() - INTERVAL '2 hours'
                            UNION ALL
                            SELECT COALESCE(NULLIF(TRIM(fcm_token), ''), anonymous_device_id) AS device_key FROM anonymous_app_devices
                            WHERE last_seen_at >= NOW() - INTERVAL '2 hours'
                        ) active_known_devices
                    ) AS active_known_device_count_recent,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM (
                            SELECT fcm_token FROM user_devices
                            WHERE platform = 'ios' AND last_seen_at >= NOW() - INTERVAL '2 hours' AND notifications_enabled = TRUE AND token_status = 'valid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                            UNION ALL
                            SELECT fcm_token FROM anonymous_app_devices
                            WHERE platform = 'ios' AND last_seen_at >= NOW() - INTERVAL '2 hours' AND notifications_enabled = TRUE AND token_status = 'valid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                        ) ios_push_ready_tokens
                    ) AS push_ready_ios_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM (
                            SELECT fcm_token FROM user_devices
                            WHERE platform = 'android' AND last_seen_at >= NOW() - INTERVAL '2 hours' AND notifications_enabled = TRUE AND token_status = 'valid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                            UNION ALL
                            SELECT fcm_token FROM anonymous_app_devices
                            WHERE platform = 'android' AND last_seen_at >= NOW() - INTERVAL '2 hours' AND notifications_enabled = TRUE AND token_status = 'valid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                        ) android_push_ready_tokens
                    ) AS push_ready_android_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM (
                            SELECT fcm_token FROM user_devices
                            WHERE last_seen_at >= NOW() - INTERVAL '2 hours' AND notifications_enabled = TRUE AND token_status = 'valid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                            UNION ALL
                            SELECT fcm_token FROM anonymous_app_devices
                            WHERE last_seen_at >= NOW() - INTERVAL '2 hours' AND notifications_enabled = TRUE AND token_status = 'valid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                        ) push_ready_tokens
                    ) AS notifications_enabled_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM (
                            SELECT fcm_token FROM user_devices
                            WHERE last_seen_at >= NOW() - INTERVAL '2 hours' AND token_status = 'valid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                            UNION ALL
                            SELECT fcm_token FROM anonymous_app_devices
                            WHERE last_seen_at >= NOW() - INTERVAL '2 hours' AND token_status = 'valid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                        ) valid_tokens
                    ) AS valid_token_count,
                    (
                        SELECT COUNT(*)
                        FROM (
                            SELECT fcm_token FROM user_devices
                            WHERE token_status = 'invalid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                            UNION ALL
                            SELECT fcm_token FROM anonymous_app_devices
                            WHERE token_status = 'invalid' AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                        ) invalid_tokens
                    ) AS invalid_token_count,
                    (SELECT COUNT(*) FROM user_devices WHERE app_version IS NULL) AS unknown_app_version_device_count,
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
                SELECT
                    u.id AS user_id,
                    u.email,
                    u.display_name,
                    u.preferred_language,
                    u.created_at AS registration_date,
                    u.last_sign_in_at,
                    COUNT(DISTINCT d.fcm_token) FILTER (
                        WHERE d.last_seen_at >= NOW() - INTERVAL '2 hours'
                          AND d.notifications_enabled = TRUE
                          AND d.token_status = 'valid'
                          AND NULLIF(TRIM(d.fcm_token), '') IS NOT NULL
                    )::int AS device_count,
                    latest_device.platform AS latest_platform,
                    latest_device.app_version AS latest_app_version,
                    latest_device.language AS latest_device_language,
                    latest_device.last_seen_at AS latest_device_last_seen_at,
                    COALESCE(BOOL_OR(
                        d.last_seen_at >= NOW() - INTERVAL '2 hours'
                        AND d.notifications_enabled = TRUE
                        AND d.token_status = 'valid'
                        AND NULLIF(TRIM(d.fcm_token), '') IS NOT NULL
                    ), FALSE) AS notifications_enabled,
                    COALESCE(a.enabled, FALSE) AS admin
                FROM users u
                LEFT JOIN user_devices d ON d.user_id = u.id
                LEFT JOIN admin_users a ON a.user_id = u.id
                LEFT JOIN LATERAL (
                    SELECT platform, app_version, language, last_seen_at
                    FROM user_devices
                    WHERE user_id = u.id
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
                    push_ready,
                    first_seen_at,
                    last_seen_at
                FROM (
                    SELECT
                        candidates.*,
                        ROW_NUMBER() OVER (
                            PARTITION BY device_key
                            ORDER BY signed_in DESC, push_ready DESC, last_seen_at DESC
                        ) AS row_number
                    FROM (
                        SELECT
                            COALESCE(NULLIF(TRIM(d.fcm_token), ''), d.id::text) AS device_key,
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
                            (
                                d.last_seen_at >= NOW() - INTERVAL '2 hours'
                                AND d.notifications_enabled = TRUE
                                AND d.token_status = 'valid'
                                AND NULLIF(TRIM(d.fcm_token), '') IS NOT NULL
                            ) AS push_ready,
                            d.created_at AS first_seen_at,
                            d.last_seen_at
                        FROM user_devices d
                        INNER JOIN users u ON u.id = d.user_id
                        UNION ALL
                        SELECT
                            COALESCE(NULLIF(TRIM(d.fcm_token), ''), d.anonymous_device_id) AS device_key,
                            d.anonymous_device_id AS id,
                            u.id AS user_id,
                            u.email AS user_email,
                            u.display_name AS user_display_name,
                            FALSE AS signed_in,
                            d.platform,
                            d.app_version,
                            d.language,
                            d.notifications_enabled,
                            d.token_status,
                            (
                                d.last_seen_at >= NOW() - INTERVAL '2 hours'
                                AND d.notifications_enabled = TRUE
                                AND d.token_status = 'valid'
                                AND NULLIF(TRIM(d.fcm_token), '') IS NOT NULL
                            ) AS push_ready,
                            d.first_seen_at,
                            d.last_seen_at
                        FROM anonymous_app_devices d
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
                rs.getBoolean("push_ready"),
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
