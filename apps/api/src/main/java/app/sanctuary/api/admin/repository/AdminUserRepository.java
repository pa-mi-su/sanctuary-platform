package app.sanctuary.api.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.admin.dto.AdminDeviceInstallDto;
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
                WITH all_devices AS (
                    SELECT
                        COALESCE(NULLIF(TRIM(client_instance_id), ''), NULLIF(TRIM(fcm_token), ''), id::text) AS install_key,
                        TRUE AS signed_in,
                        platform,
                        fcm_token,
                        notifications_enabled,
                        token_status,
                        app_version,
                        last_seen_at,
                        updated_at
                    FROM user_devices
                    WHERE automated_test = FALSE
                      AND check_in_source = 'app'
                      AND (
                          NULLIF(TRIM(client_instance_id), '') IS NOT NULL
                          OR NULLIF(TRIM(fcm_token), '') IS NOT NULL
                      )
                    UNION ALL
                    SELECT
                        COALESCE(NULLIF(TRIM(client_instance_id), ''), NULLIF(TRIM(fcm_token), ''), anonymous_device_id) AS install_key,
                        linked_user_id IS NOT NULL AS signed_in,
                        platform,
                        fcm_token,
                        notifications_enabled,
                        token_status,
                        app_version,
                        last_seen_at,
                        updated_at
                    FROM anonymous_app_devices
                    WHERE automated_test = FALSE
                      AND check_in_source = 'app'
                      AND (
                          NULLIF(TRIM(client_instance_id), '') IS NOT NULL
                          OR NULLIF(TRIM(fcm_token), '') IS NOT NULL
                      )
                ),
                deduped_devices AS (
                    SELECT *
                    FROM (
                        SELECT
                            all_devices.*,
                            ROW_NUMBER() OVER (
                                PARTITION BY install_key
                                ORDER BY signed_in DESC, last_seen_at DESC, updated_at DESC
                            ) AS row_number
                        FROM all_devices
                    ) ranked
                    WHERE row_number = 1
                )
                SELECT
                    (SELECT COUNT(*) FROM users) AS total_users,
                    (SELECT COUNT(*) FROM users WHERE created_at >= NOW() - INTERVAL '1 day') AS registered_users_today,
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '1 day') AS active_users_today,
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '30 days') AS active_users_30_days,
                    (
                        SELECT COUNT(*)
                        FROM deduped_devices
                        WHERE signed_in = FALSE
                          AND last_seen_at >= NOW() - INTERVAL '1 day'
                    ) AS anonymous_active_devices_today,
                    (
                        SELECT COUNT(*)
                        FROM deduped_devices
                        WHERE signed_in = FALSE
                          AND last_seen_at >= NOW() - INTERVAL '7 days'
                    ) AS anonymous_active_devices_7_days,
                    (SELECT COUNT(*) FROM deduped_devices) AS known_app_install_count,
                    (
                        SELECT COUNT(*)
                        FROM deduped_devices
                        WHERE last_seen_at >= NOW() - INTERVAL '5 minutes'
                    ) AS active_known_device_count_recent,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM deduped_devices
                        WHERE platform = 'ios'
                          AND notifications_enabled = TRUE
                          AND token_status = 'valid'
                          AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                    ) AS push_ready_ios_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM deduped_devices
                        WHERE platform = 'android'
                          AND notifications_enabled = TRUE
                          AND token_status = 'valid'
                          AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                    ) AS push_ready_android_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM deduped_devices
                        WHERE notifications_enabled = TRUE
                          AND token_status = 'valid'
                          AND NULLIF(TRIM(fcm_token), '') IS NOT NULL
                    ) AS notifications_enabled_device_count,
                    (
                        SELECT COUNT(DISTINCT fcm_token)
                        FROM deduped_devices
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
                    (SELECT COUNT(*) FROM deduped_devices WHERE app_version IS NULL) AS unknown_app_version_device_count,
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

    public List<AdminDeviceInstallDto> listRecentDeviceInstalls(int limit) {
        return jdbcTemplate.query(
            """
                WITH all_devices AS (
                    SELECT
                        COALESCE(NULLIF(TRIM(d.client_instance_id), ''), NULLIF(TRIM(d.fcm_token), ''), d.id::text) AS device_key,
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
                        d.last_seen_at,
                        d.updated_at
                    FROM user_devices d
                    INNER JOIN users u ON u.id = d.user_id
                    WHERE d.automated_test = FALSE
                      AND d.check_in_source = 'app'
                    UNION ALL
                    SELECT
                        COALESCE(NULLIF(TRIM(d.client_instance_id), ''), NULLIF(TRIM(d.fcm_token), ''), d.anonymous_device_id) AS device_key,
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
                        d.last_seen_at,
                        d.updated_at
                    FROM anonymous_app_devices d
                    LEFT JOIN users u ON u.id = d.linked_user_id
                    WHERE d.automated_test = FALSE
                      AND d.check_in_source = 'app'
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
                        all_devices.*,
                        ROW_NUMBER() OVER (
                            PARTITION BY device_key
                            ORDER BY signed_in DESC, push_ready DESC, last_seen_at DESC, updated_at DESC
                        ) AS row_number
                    FROM all_devices
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

}
