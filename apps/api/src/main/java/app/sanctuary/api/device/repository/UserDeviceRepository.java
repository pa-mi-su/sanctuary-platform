package app.sanctuary.api.device.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.device.dto.UserDeviceDto;
import app.sanctuary.api.device.dto.UserDeviceRegistrationRequest;

@Repository
public class UserDeviceRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserDeviceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserDeviceDto upsert(UUID userId, UserDeviceRegistrationRequest request) {
        return jdbcTemplate.queryForObject(
            """
                INSERT INTO user_devices (
                    user_id,
                    fcm_token,
                    platform,
                    app_version,
                    language,
                    notifications_enabled,
                    token_status,
                    last_seen_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'valid', NOW(), NOW())
                ON CONFLICT (fcm_token)
                DO UPDATE SET
                    user_id = EXCLUDED.user_id,
                    platform = EXCLUDED.platform,
                    app_version = EXCLUDED.app_version,
                    language = EXCLUDED.language,
                    notifications_enabled = EXCLUDED.notifications_enabled,
                    token_status = 'valid',
                    last_seen_at = NOW(),
                    updated_at = NOW()
                RETURNING
                    id,
                    user_id,
                    platform,
                    app_version,
                    language,
                    notifications_enabled,
                    token_status,
                    last_seen_at,
                    created_at,
                    updated_at
                """,
            (rs, rowNum) -> new UserDeviceDto(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("platform"),
                rs.getString("app_version"),
                rs.getString("language"),
                rs.getBoolean("notifications_enabled"),
                rs.getString("token_status"),
                rs.getObject("last_seen_at", java.time.OffsetDateTime.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            userId,
            request.fcmToken().trim(),
            request.platform(),
            emptyToNull(request.appVersion()),
            request.language(),
            request.notificationsEnabled()
        );
    }

    public List<UserDeviceDto> findByUserId(UUID userId) {
        return jdbcTemplate.query(
            """
                SELECT
                    id,
                    user_id,
                    platform,
                    app_version,
                    language,
                    notifications_enabled,
                    token_status,
                    last_seen_at,
                    created_at,
                    updated_at
                FROM user_devices
                WHERE user_id = ?
                ORDER BY updated_at DESC
                """,
            (rs, rowNum) -> new UserDeviceDto(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("platform"),
                rs.getString("app_version"),
                rs.getString("language"),
                rs.getBoolean("notifications_enabled"),
                rs.getString("token_status"),
                rs.getObject("last_seen_at", java.time.OffsetDateTime.class),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            userId
        );
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
