package app.sanctuary.api.calendar.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import app.sanctuary.api.calendar.model.NovenaServingRule;

class NovenaServingRuleDatabaseAuditTest {

    private static final int MIN_SUPPORTED_YEAR = 1900;
    private static final int MAX_SUPPORTED_YEAR = 4099;

    private final NovenaServingWindowResolver resolver =
        new NovenaServingWindowResolver(new LiturgicalAnchorService(new TransferredFeastResolver()));

    @Test
    void everyStoredNovenaServingRuleResolvesAcrossEverySupportedYear() throws SQLException {
        String databaseUrl = System.getenv().getOrDefault(
            "SANCTUARY_TEST_DATABASE_URL",
            "jdbc:postgresql://localhost:5432/sanctuary?user=sanctuary&password=change-me-now"
        );

        List<NovenaServingRule> rules;
        try {
            rules = loadRules(databaseUrl);
        } catch (SQLException ex) {
            Assumptions.abort("No local Sanctuary test database available: " + ex.getMessage());
            return;
        }

        assertFalse(rules.isEmpty(), "Novena serving rule table should not be empty");
        for (NovenaServingRule rule : rules) {
            assertAllowedRuleType(rule.startRuleType(), rule.novenaId(), "start");
            assertAllowedRuleType(rule.feastRuleType(), rule.novenaId(), "feast");
            assertValidWeekday(rule.startRuleWeekday(), rule.novenaId(), "start");
            assertValidWeekday(rule.feastRuleWeekday(), rule.novenaId(), "feast");

            for (int year = MIN_SUPPORTED_YEAR; year <= MAX_SUPPORTED_YEAR; year++) {
                var window = resolver.resolve(rule, year);
                assertNotNull(window.feastDate(), rule.novenaId() + " feast " + year);
                assertFalse(window.startDate().isAfter(window.endDate()), rule.novenaId() + " start/end " + year);
                assertTrue(window.endDate().isBefore(window.feastDate()) || window.endDate().equals(window.startDate()),
                    rule.novenaId() + " should end before feast unless it is a same-day window " + year);
                assertTrue(window.startDate().isAfter(LocalDate.of(year - 1, 1, 1)), rule.novenaId() + " start " + year);
                assertTrue(window.feastDate().isBefore(LocalDate.of(year + 1, 1, 1)), rule.novenaId() + " feast " + year);
            }
        }
    }

    private List<NovenaServingRule> loadRules(String databaseUrl) throws SQLException {
        try (
            var connection = DriverManager.getConnection(databaseUrl);
            var statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("""
                SELECT
                  nsr.novena_id,
                  nsr.start_rule_type,
                  nsr.start_rule_month,
                  nsr.start_rule_day,
                  nsr.start_rule_anchor,
                  nsr.start_rule_offset_days,
                  nsr.start_rule_weekday,
                  nsr.start_rule_weekday_policy,
                  nsr.start_rule_n,
                  nsr.start_rule_days_before,
                  nsr.feast_rule_type,
                  nsr.feast_rule_month,
                  nsr.feast_rule_day,
                  nsr.feast_rule_anchor,
                  nsr.feast_rule_offset_days,
                  nsr.feast_rule_weekday,
                  nsr.feast_rule_weekday_policy,
                  nsr.feast_rule_n,
                  nsr.feast_rule_days_before,
                  nsr.entry_duration_days,
                  n.duration_days AS source_duration_days
                FROM novena_serving_rules nsr
                JOIN novenas n ON n.id = nsr.novena_id
                ORDER BY nsr.novena_id
                """)
        ) {
            List<NovenaServingRule> rules = new ArrayList<>();
            while (rs.next()) {
                rules.add(new NovenaServingRule(
                    rs.getString("novena_id"),
                    rs.getString("start_rule_type"),
                    nullableInt(rs, "start_rule_month"),
                    nullableInt(rs, "start_rule_day"),
                    rs.getString("start_rule_anchor"),
                    nullableInt(rs, "start_rule_offset_days"),
                    nullableInt(rs, "start_rule_weekday"),
                    rs.getString("start_rule_weekday_policy"),
                    nullableInt(rs, "start_rule_n"),
                    nullableInt(rs, "start_rule_days_before"),
                    rs.getString("feast_rule_type"),
                    nullableInt(rs, "feast_rule_month"),
                    nullableInt(rs, "feast_rule_day"),
                    rs.getString("feast_rule_anchor"),
                    nullableInt(rs, "feast_rule_offset_days"),
                    nullableInt(rs, "feast_rule_weekday"),
                    rs.getString("feast_rule_weekday_policy"),
                    nullableInt(rs, "feast_rule_n"),
                    nullableInt(rs, "feast_rule_days_before"),
                    nullableInt(rs, "entry_duration_days"),
                    nullableInt(rs, "source_duration_days")
                ));
            }
            return rules;
        }
    }

    private Integer nullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private void assertAllowedRuleType(String type, String novenaId, String field) {
        assertTrue(
            type == null
                || type.equals("fixed")
                || type.equals("anchor")
                || type.equals("relative")
                || type.equals("nth_weekday_after")
                || type.equals("before_feast"),
            novenaId + " has unsupported " + field + " rule type: " + type
        );
    }

    private void assertValidWeekday(Integer weekday, String novenaId, String field) {
        assertTrue(
            weekday == null || (weekday >= 0 && weekday <= 6),
            novenaId + " has invalid " + field + " weekday: " + weekday
        );
    }
}
