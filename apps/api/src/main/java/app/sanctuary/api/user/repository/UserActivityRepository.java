package app.sanctuary.api.user.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserActivityRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordActivity(
        UUID userId,
        String activityType,
        String resourceType,
        String resourceId,
        LocalDate activityDate,
        OffsetDateTime occurredAt
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO user_activity_events (
                    user_id,
                    activity_type,
                    resource_type,
                    resource_id,
                    activity_date,
                    occurred_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
            userId,
            activityType,
            resourceType,
            resourceId,
            Date.valueOf(activityDate),
            occurredAt
        );
    }

    public List<LocalDate> findDistinctActivityDates(UUID userId) {
        return jdbcTemplate.query(
            """
                SELECT DISTINCT activity_date
                FROM user_activity_events
                WHERE user_id = ?
                ORDER BY activity_date ASC
                """,
            (rs, rowNum) -> rs.getObject("activity_date", LocalDate.class),
            userId
        );
    }
}
