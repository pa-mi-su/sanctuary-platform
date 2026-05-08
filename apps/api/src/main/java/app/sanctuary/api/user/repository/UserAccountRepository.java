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

    public UserAccountDto upsert(String cognitoSub, String email, String firstName, String lastName, String displayName, String avatarUrl) {
        return jdbcTemplate.queryForObject(
            """
                INSERT INTO users (
                    cognito_sub,
                    email,
                    first_name,
                    last_name,
                    display_name,
                    avatar_url,
                    last_sign_in_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                ON CONFLICT (cognito_sub)
                DO UPDATE SET
                    email = EXCLUDED.email,
                    first_name = COALESCE(EXCLUDED.first_name, users.first_name),
                    last_name = COALESCE(EXCLUDED.last_name, users.last_name),
                    display_name = EXCLUDED.display_name,
                    avatar_url = COALESCE(EXCLUDED.avatar_url, users.avatar_url),
                    last_sign_in_at = NOW(),
                    updated_at = NOW()
                RETURNING
                    id,
                    cognito_sub,
                    email,
                    first_name,
                    last_name,
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
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("display_name"),
                rs.getString("preferred_language"),
                rs.getString("avatar_url"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            cognitoSub,
            emptyToNull(email),
            emptyToNull(firstName),
            emptyToNull(lastName),
            emptyToNull(displayName),
            emptyToNull(avatarUrl)
        );
    }

    public boolean isDeletedIdentity(String cognitoSub, String emailHash) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM deleted_user_accounts
                WHERE (cognito_sub IS NOT NULL AND cognito_sub = ?)
                   OR (email_hash IS NOT NULL AND email_hash = ?)
                """,
            Integer.class,
            emptyToNull(cognitoSub),
            emptyToNull(emailHash)
        );

        return count != null && count > 0;
    }

    public Optional<UserAccountDto> findByCognitoSub(String cognitoSub) {
        return jdbcTemplate.query(
            """
                SELECT
                    id,
                    cognito_sub,
                    email,
                    first_name,
                    last_name,
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
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("display_name"),
                rs.getString("preferred_language"),
                rs.getString("avatar_url"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            cognitoSub
        ).stream().findFirst();
    }

    public UserAccountDto updatePreferredLanguage(UUID userId, String preferredLanguage) {
        return jdbcTemplate.queryForObject(
            """
                UPDATE users
                SET
                    preferred_language = ?,
                    updated_at = NOW()
                WHERE id = ?
                RETURNING
                    id,
                    cognito_sub,
                    email,
                    first_name,
                    last_name,
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
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("display_name"),
                rs.getString("preferred_language"),
                rs.getString("avatar_url"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
            ),
            preferredLanguage,
            userId
        );
    }

    public void deleteById(UUID userId) {
        jdbcTemplate.update(
            """
                DELETE FROM users
                WHERE id = ?
                """,
            userId
        );
    }

    public void markDeleted(String cognitoSub, String emailHash) {
        jdbcTemplate.update(
            """
                INSERT INTO deleted_user_accounts (
                    cognito_sub,
                    email_hash,
                    deleted_at
                )
                VALUES (?, ?, NOW())
                ON CONFLICT (cognito_sub)
                DO UPDATE SET
                    email_hash = COALESCE(EXCLUDED.email_hash, deleted_user_accounts.email_hash),
                    deleted_at = NOW()
                """,
            emptyToNull(cognitoSub),
            emptyToNull(emailHash)
        );
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
