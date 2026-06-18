package app.sanctuary.api.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '1 day') AS active_users_today,
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '7 days') AS active_users_7_days,
                    (SELECT COUNT(*) FROM users WHERE last_sign_in_at >= NOW() - INTERVAL '30 days') AS active_users_30_days,
                    (SELECT COUNT(*) FROM user_devices) AS device_count,
                    (SELECT COUNT(*) FROM user_devices WHERE last_seen_at >= NOW() - INTERVAL '7 days') AS active_devices_7_days,
                    (SELECT COUNT(*) FROM user_devices WHERE last_seen_at >= NOW() - INTERVAL '30 days') AS active_devices_30_days,
                    (SELECT COUNT(*) FROM user_devices WHERE platform = 'ios') AS ios_device_count,
                    (SELECT COUNT(*) FROM user_devices WHERE platform = 'android') AS android_device_count,
                    (SELECT COUNT(*) FROM user_devices WHERE language = 'en') AS english_device_count,
                    (SELECT COUNT(*) FROM user_devices WHERE language = 'es') AS spanish_device_count,
                    (SELECT COUNT(*) FROM user_devices WHERE language = 'pl') AS polish_device_count,
                    (SELECT COUNT(*) FROM user_devices WHERE notifications_enabled = TRUE AND token_status = 'valid') AS notifications_enabled_device_count,
                    (SELECT COUNT(*) FROM user_devices WHERE token_status = 'invalid') AS invalid_token_count,
                    (SELECT COUNT(*) FROM user_devices WHERE app_version IS NULL) AS unknown_app_version_device_count
                """,
            (rs, rowNum) -> new AdminUserMetricsDto(
                rs.getInt("total_users"),
                rs.getInt("active_users_today"),
                rs.getInt("active_users_7_days"),
                rs.getInt("active_users_30_days"),
                rs.getInt("device_count"),
                rs.getInt("active_devices_7_days"),
                rs.getInt("active_devices_30_days"),
                rs.getInt("ios_device_count"),
                rs.getInt("android_device_count"),
                rs.getInt("english_device_count"),
                rs.getInt("spanish_device_count"),
                rs.getInt("polish_device_count"),
                rs.getInt("notifications_enabled_device_count"),
                rs.getInt("invalid_token_count"),
                rs.getInt("unknown_app_version_device_count")
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
                    COUNT(d.id)::int AS device_count,
                    latest_device.platform AS latest_platform,
                    latest_device.app_version AS latest_app_version,
                    latest_device.language AS latest_device_language,
                    latest_device.last_seen_at AS latest_device_last_seen_at,
                    COALESCE(BOOL_OR(d.notifications_enabled AND d.token_status = 'valid'), FALSE) AS notifications_enabled
                FROM users u
                LEFT JOIN user_devices d ON d.user_id = u.id
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
                    latest_device.last_seen_at
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
                rs.getBoolean("notifications_enabled")
            ),
            limit
        );
    }
}
