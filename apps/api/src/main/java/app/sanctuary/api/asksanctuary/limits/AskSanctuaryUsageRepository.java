package app.sanctuary.api.asksanctuary.limits;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AskSanctuaryUsageRepository {

    private final JdbcTemplate jdbcTemplate;

    public AskSanctuaryUsageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AskSanctuaryEntitlement findEntitlement(UUID userId) {
        return jdbcTemplate.query(
            """
                SELECT tier, daily_limit_override, unlimited
                FROM ask_sanctuary_user_entitlements
                WHERE user_id = ?
                """,
            (rs, rowNum) -> new AskSanctuaryEntitlement(
                rs.getString("tier"),
                (Integer) rs.getObject("daily_limit_override"),
                rs.getBoolean("unlimited")
            ),
            userId
        ).stream().findFirst().orElseGet(AskSanctuaryEntitlement::free);
    }

    public int recordAttemptAndCountRecent(UUID userId, OffsetDateTime cutoff) {
        jdbcTemplate.update(
            """
                DELETE FROM ask_sanctuary_request_events
                WHERE created_at < ?
                """,
            cutoff
        );
        jdbcTemplate.update(
            """
                INSERT INTO ask_sanctuary_request_events (user_id)
                VALUES (?)
                """,
            userId
        );

        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM ask_sanctuary_request_events
                WHERE user_id = ?
                  AND created_at >= ?
                """,
            Integer.class,
            userId,
            cutoff
        );
        return count == null ? 0 : count;
    }

    public Optional<Integer> reserveDailyRequest(UUID userId, LocalDate usageDate, int dailyLimit) {
        return jdbcTemplate.query(
            """
                INSERT INTO ask_sanctuary_daily_usage (
                    user_id,
                    usage_date,
                    request_count,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, 1, NOW(), NOW())
                ON CONFLICT (user_id, usage_date)
                DO UPDATE SET
                    request_count = ask_sanctuary_daily_usage.request_count + 1,
                    updated_at = NOW()
                WHERE ask_sanctuary_daily_usage.request_count < ?
                RETURNING request_count
                """,
            (rs, rowNum) -> rs.getInt("request_count"),
            userId,
            usageDate,
            dailyLimit
        ).stream().findFirst();
    }

    public Optional<Integer> reserveDailyIpRequest(String ipHash, LocalDate usageDate, int dailyLimit) {
        if (ipHash == null || ipHash.isBlank()) {
            return Optional.of(0);
        }

        return jdbcTemplate.query(
            """
                INSERT INTO ask_sanctuary_ip_daily_usage (
                    ip_hash,
                    usage_date,
                    request_count,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, 1, NOW(), NOW())
                ON CONFLICT (ip_hash, usage_date)
                DO UPDATE SET
                    request_count = ask_sanctuary_ip_daily_usage.request_count + 1,
                    updated_at = NOW()
                WHERE ask_sanctuary_ip_daily_usage.request_count < ?
                RETURNING request_count
                """,
            (rs, rowNum) -> rs.getInt("request_count"),
            ipHash,
            usageDate,
            dailyLimit
        ).stream().findFirst();
    }

    public Optional<OffsetDateTime> findActiveLockUntil(UUID userId, OffsetDateTime now) {
        return jdbcTemplate.query(
            """
                SELECT locked_until
                FROM ask_sanctuary_account_locks
                WHERE user_id = ?
                  AND locked_until > ?
                ORDER BY locked_until DESC
                LIMIT 1
                """,
            (rs, rowNum) -> rs.getObject("locked_until", OffsetDateTime.class),
            userId,
            now
        ).stream().findFirst();
    }

    public int recordMisuseAndCountRecent(UUID userId, String guardrailType, OffsetDateTime cutoff) {
        jdbcTemplate.update(
            """
                DELETE FROM ask_sanctuary_misuse_events
                WHERE created_at < ?
                """,
            cutoff
        );
        jdbcTemplate.update(
            """
                INSERT INTO ask_sanctuary_misuse_events (
                    user_id,
                    guardrail_type
                )
                VALUES (?, ?)
                """,
            userId,
            guardrailType
        );

        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM ask_sanctuary_misuse_events
                WHERE user_id = ?
                  AND created_at >= ?
                """,
            Integer.class,
            userId,
            cutoff
        );
        return count == null ? 0 : count;
    }

    public void createLock(UUID userId, String reason, OffsetDateTime lockedUntil) {
        jdbcTemplate.update(
            """
                INSERT INTO ask_sanctuary_account_locks (
                    user_id,
                    reason,
                    locked_until
                )
                VALUES (?, ?, ?)
                """,
            userId,
            reason,
            lockedUntil
        );
    }
}
