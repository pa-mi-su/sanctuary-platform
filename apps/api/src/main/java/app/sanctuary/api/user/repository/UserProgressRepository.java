package app.sanctuary.api.user.repository;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.user.dto.UserFavoriteDto;
import app.sanctuary.api.user.dto.UserNovenaCommitmentDto;
import app.sanctuary.api.user.dto.UserNovenaCommitmentRequest;

@Repository
public class UserProgressRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserProgressRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<UserFavoriteDto> findFavorites(String userId) {
        return jdbcTemplate.query(
            """
                SELECT item_type, item_id, created_at
                FROM user_favorites
                WHERE user_id = ?
                ORDER BY created_at DESC
                """,
            (rs, rowNum) -> new UserFavoriteDto(
                rs.getString("item_type"),
                rs.getString("item_id"),
                rs.getObject("created_at", OffsetDateTime.class)
            ),
            userId
        );
    }

    public void saveFavorite(String userId, String itemType, String itemId) {
        jdbcTemplate.update(
            """
                INSERT INTO user_favorites (user_id, item_type, item_id)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, item_type, item_id) DO NOTHING
                """,
            userId,
            itemType,
            itemId
        );
    }

    public void deleteFavorite(String userId, String itemType, String itemId) {
        jdbcTemplate.update(
            """
                DELETE FROM user_favorites
                WHERE user_id = ? AND item_type = ? AND item_id = ?
                """,
            userId,
            itemType,
            itemId
        );
    }

    public List<UserNovenaCommitmentDto> findNovenaCommitments(String userId) {
        return jdbcTemplate.query(
            """
                SELECT
                    novena_id,
                    started_at,
                    current_day,
                    completed_days,
                    reminder_enabled,
                    reminder_morning_hour,
                    reminder_evening_hour,
                    reminder_time_zone_id,
                    status,
                    updated_at
                FROM user_novena_commitments
                WHERE user_id = ?
                ORDER BY updated_at DESC
                """,
            (rs, rowNum) -> new UserNovenaCommitmentDto(
                rs.getString("novena_id"),
                rs.getObject("started_at", OffsetDateTime.class),
                rs.getInt("current_day"),
                toIntegerList(rs.getArray("completed_days")),
                rs.getBoolean("reminder_enabled"),
                (Integer) rs.getObject("reminder_morning_hour"),
                (Integer) rs.getObject("reminder_evening_hour"),
                rs.getString("reminder_time_zone_id"),
                rs.getString("status"),
                rs.getObject("updated_at", OffsetDateTime.class)
            ),
            userId
        );
    }

    public UserNovenaCommitmentDto saveNovenaCommitment(String userId, String novenaId, UserNovenaCommitmentRequest request) {
        OffsetDateTime startedAt = request.startedAt() == null ? OffsetDateTime.now() : request.startedAt();
        OffsetDateTime updatedAt = OffsetDateTime.now();
        jdbcTemplate.update(
            connection -> {
                var statement = connection.prepareStatement(
                    """
                        INSERT INTO user_novena_commitments (
                            user_id,
                            novena_id,
                            started_at,
                            current_day,
                            completed_days,
                            reminder_enabled,
                            reminder_morning_hour,
                            reminder_evening_hour,
                            reminder_time_zone_id,
                            status,
                            updated_at
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (user_id, novena_id)
                        DO UPDATE SET
                            started_at = EXCLUDED.started_at,
                            current_day = EXCLUDED.current_day,
                            completed_days = EXCLUDED.completed_days,
                            reminder_enabled = EXCLUDED.reminder_enabled,
                            reminder_morning_hour = EXCLUDED.reminder_morning_hour,
                            reminder_evening_hour = EXCLUDED.reminder_evening_hour,
                            reminder_time_zone_id = EXCLUDED.reminder_time_zone_id,
                            status = EXCLUDED.status,
                            updated_at = EXCLUDED.updated_at
                        """
                );
                statement.setString(1, userId);
                statement.setString(2, novenaId);
                statement.setTimestamp(3, Timestamp.from(startedAt.toInstant()));
                statement.setInt(4, request.currentDay());
                statement.setArray(5, connection.createArrayOf("INTEGER", request.completedDays().toArray(Integer[]::new)));
                statement.setBoolean(6, request.reminderEnabled());
                statement.setObject(7, request.reminderMorningHour());
                statement.setObject(8, request.reminderEveningHour());
                statement.setString(9, request.reminderTimeZoneId());
                statement.setString(10, request.status());
                statement.setTimestamp(11, Timestamp.from(updatedAt.toInstant()));
                return statement;
            }
        );

        return new UserNovenaCommitmentDto(
            novenaId,
            startedAt,
            request.currentDay(),
            request.completedDays(),
            request.reminderEnabled(),
            request.reminderMorningHour(),
            request.reminderEveningHour(),
            request.reminderTimeZoneId(),
            request.status(),
            updatedAt
        );
    }

    public void deleteNovenaCommitment(String userId, String novenaId) {
        jdbcTemplate.update(
            """
                DELETE FROM user_novena_commitments
                WHERE user_id = ? AND novena_id = ?
                """,
            userId,
            novenaId
        );
    }

    private List<Integer> toIntegerList(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }

        Object value = array.getArray();
        if (value instanceof Integer[] integers) {
            return Arrays.asList(integers);
        }

        return List.of();
    }
}
