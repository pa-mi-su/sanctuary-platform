package app.sanctuary.api.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.user.dto.UserAccountDto;

@Repository
public class UserAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UserAccountDto upsert(String cognitoSub, String email, String firstName, String lastName, String displayName, String avatarUrl) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail != null) {
            Optional<UserAccountDto> existingEmailAccount = updateByEmail(
                cognitoSub,
                normalizedEmail,
                firstName,
                lastName,
                displayName,
                avatarUrl
            );
            if (existingEmailAccount.isPresent()) {
                return existingEmailAccount.get();
            }
        }

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
            accountRowMapper(),
            cognitoSub,
            normalizedEmail,
            emptyToNull(firstName),
            emptyToNull(lastName),
            emptyToNull(displayName),
            emptyToNull(avatarUrl)
        );
    }

    private Optional<UserAccountDto> updateByEmail(
        String cognitoSub,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String avatarUrl
    ) {
        return jdbcTemplate.query(
            """
                UPDATE users
                SET
                    cognito_sub = ?,
                    email = ?,
                    first_name = COALESCE(?, users.first_name),
                    last_name = COALESCE(?, users.last_name),
                    display_name = COALESCE(?, users.display_name),
                    avatar_url = COALESCE(?, users.avatar_url),
                    last_sign_in_at = NOW(),
                    updated_at = NOW()
                WHERE LOWER(email) = LOWER(?)
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
            accountRowMapper(),
            cognitoSub,
            email,
            emptyToNull(firstName),
            emptyToNull(lastName),
            emptyToNull(displayName),
            emptyToNull(avatarUrl),
            email
        ).stream().findFirst();
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
            accountRowMapper(),
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
            accountRowMapper(),
            preferredLanguage,
            userId
        );
    }

    public void deleteById(UUID userId) {
        deleteOwnedUserData(userId);
        jdbcTemplate.update(
            """
                DELETE FROM users
                WHERE id = ?
                """,
            userId
        );
    }

    private void deleteOwnedUserData(UUID userId) {
        jdbcTemplate.update(
            """
                DELETE FROM user_activity_events
                WHERE user_id = ?
                """,
            userId
        );
        jdbcTemplate.update(
            """
                DELETE FROM user_preferences
                WHERE user_id = ?
                """,
            userId
        );
        jdbcTemplate.update(
            """
                DELETE FROM user_novena_commitments
                WHERE user_id = ?
                """,
            userId
        );
        jdbcTemplate.update(
            """
                DELETE FROM user_favorites
                WHERE user_id = ?
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

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private RowMapper<UserAccountDto> accountRowMapper() {
        return (rs, rowNum) -> new UserAccountDto(
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
        );
    }
}
