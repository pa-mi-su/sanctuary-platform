package app.sanctuary.api.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;

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
                    delivered_count,
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
                    delivered_count,
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

    private AdminNotificationDto mapNotification(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AdminNotificationDto(
            rs.getObject("id", UUID.class),
            rs.getString("title"),
            rs.getString("message"),
            rs.getString("audience_type"),
            rs.getString("status"),
            rs.getInt("target_count"),
            rs.getInt("delivered_count"),
            rs.getInt("failed_count"),
            rs.getObject("sent_at", java.time.OffsetDateTime.class),
            rs.getObject("created_at", java.time.OffsetDateTime.class),
            rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}
