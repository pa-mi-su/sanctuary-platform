package app.sanctuary.api.admin.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(UUID actorUserId, String action, String targetType, String targetId) {
        jdbcTemplate.update(
            """
                INSERT INTO admin_audit_events (
                    actor_user_id,
                    action,
                    target_type,
                    target_id
                )
                VALUES (?, ?, ?, ?)
                """,
            actorUserId,
            action,
            targetType,
            targetId
        );
    }
}
