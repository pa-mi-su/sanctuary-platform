package app.sanctuary.api.admin.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAuthorizationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminAuthorizationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isAdmin(UUID userId) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM admin_users
                WHERE user_id = ?
                  AND enabled = TRUE
                """,
            Integer.class,
            userId
        );
        return count != null && count > 0;
    }
}
