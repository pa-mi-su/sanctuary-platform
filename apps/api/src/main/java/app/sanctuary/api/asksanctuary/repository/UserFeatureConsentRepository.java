package app.sanctuary.api.asksanctuary.repository;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserFeatureConsentRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserFeatureConsentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean hasAccepted(UUID userId, String feature, String version) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM user_feature_consents
                WHERE user_id = ?
                  AND feature = ?
                  AND version = ?
                """,
            Integer.class,
            userId,
            feature,
            version
        );
        return count != null && count > 0;
    }

    public void accept(UUID userId, String feature, String version) {
        jdbcTemplate.update(
            """
                INSERT INTO user_feature_consents (
                    user_id,
                    feature,
                    version,
                    accepted_at,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, NOW(), NOW(), NOW())
                ON CONFLICT (user_id, feature, version)
                DO UPDATE SET
                    accepted_at = user_feature_consents.accepted_at,
                    updated_at = NOW()
                """,
            userId,
            feature,
            version
        );
    }
}
