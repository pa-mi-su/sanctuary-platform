package app.sanctuary.api.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.user.dto.UserAccountDto;

@Repository
public class UserAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserAccountDto upsert(String cognitoSub, String email, String displayName, String avatarUrl) {
        return jdbcTemplate.queryForObject(
            """
                INSERT INTO users (
                    cognito_sub,
                    email,
                    display_name,
                    avatar_url,
                    last_sign_in_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, NOW(), NOW())
                ON CONFLICT (cognito_sub)
                DO UPDATE SET
                    email = EXCLUDED.email,
                    display_name = EXCLUDED.display_name,
                    avatar_url = COALESCE(EXCLUDED.avatar_url, users.avatar_url),
                    last_sign_in_at = NOW(),
                    updated_at = NOW()
                RETURNING
                    id,
                    cognito_sub,
                    email,
                    display_name,
                    preferred_language,
                    avatar_url,
                    created_at,
                    updated_at
                """,
            (rs, rowNum) -> new UserAccountDto(
                rs.getObject("id", UUID.class),
                rs.getString("cognito_sub"),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("preferred_language"),
                rs.getString("avatar_url"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            cognitoSub,
            emptyToNull(email),
            emptyToNull(displayName),
            emptyToNull(avatarUrl)
        );
    }

    public Optional<UserAccountDto> findByCognitoSub(String cognitoSub) {
        return jdbcTemplate.query(
            """
                SELECT
                    id,
                    cognito_sub,
                    email,
                    display_name,
                    preferred_language,
                    avatar_url,
                    created_at,
                    updated_at
                FROM users
                WHERE cognito_sub = ?
                """,
            (rs, rowNum) -> new UserAccountDto(
                rs.getObject("id", UUID.class),
                rs.getString("cognito_sub"),
                rs.getString("email"),
                rs.getString("display_name"),
                rs.getString("preferred_language"),
                rs.getString("avatar_url"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            cognitoSub
        ).stream().findFirst();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
