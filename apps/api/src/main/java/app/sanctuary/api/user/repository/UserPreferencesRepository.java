package app.sanctuary.api.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.user.dto.UserPreferenceDto;
import app.sanctuary.api.user.dto.UserPreferencesUpdateRequest;

@Repository
public class UserPreferencesRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserPreferencesRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserPreferenceDto ensureForUser(UUID userId) {
        jdbcTemplate.update(
            """
                INSERT INTO user_preferences (user_id)
                VALUES (?)
                ON CONFLICT (user_id) DO NOTHING
                """,
            userId
        );

        return findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("User preferences should exist after initialization."));
    }

    public Optional<UserPreferenceDto> findByUserId(UUID userId) {
        return jdbcTemplate.query(
            """
                SELECT
                    user_id,
                    time_zone_id,
                    novena_reminders_enabled,
                    feast_reminders_enabled,
                    email_updates_enabled,
                    onboarding_completed,
                    created_at,
                    updated_at
                FROM user_preferences
                WHERE user_id = ?
                """,
            (rs, rowNum) -> new UserPreferenceDto(
                rs.getObject("user_id", UUID.class),
                rs.getString("time_zone_id"),
                rs.getBoolean("novena_reminders_enabled"),
                rs.getBoolean("feast_reminders_enabled"),
                rs.getBoolean("email_updates_enabled"),
                rs.getBoolean("onboarding_completed"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            userId
        ).stream().findFirst();
    }

    public UserPreferenceDto update(UUID userId, UserPreferencesUpdateRequest request) {
        ensureForUser(userId);

        jdbcTemplate.update(
            """
                UPDATE user_preferences
                SET
                    time_zone_id = ?,
                    novena_reminders_enabled = ?,
                    feast_reminders_enabled = ?,
                    email_updates_enabled = ?,
                    onboarding_completed = ?,
                    updated_at = NOW()
                WHERE user_id = ?
                """,
            request.timeZoneId(),
            request.novenaRemindersEnabled(),
            request.feastRemindersEnabled(),
            request.emailUpdatesEnabled(),
            request.onboardingCompleted(),
            userId
        );

        return findByUserId(userId)
            .orElseThrow(() -> new IllegalStateException("Updated user preferences could not be loaded."));
    }
}
