package app.sanctuary.api.calendar.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.calendar.model.NovenaServingRule;

@Repository
public class NovenaServingRuleRepository {

    private final JdbcTemplate jdbcTemplate;

    public NovenaServingRuleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<NovenaServingRule> findByNovenaId(String novenaId) {
        return jdbcTemplate.query(
            baseSelectSql() + " where nsr.novena_id = ?",
            this::mapRow,
            novenaId
        ).stream().findFirst();
    }

    public List<NovenaServingRule> findAll() {
        return jdbcTemplate.query(baseSelectSql(), this::mapRow);
    }

    private NovenaServingRule mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new NovenaServingRule(
            rs.getString("novena_id"),
            rs.getString("start_rule_type"),
            getNullableInt(rs, "start_rule_month"),
            getNullableInt(rs, "start_rule_day"),
            rs.getString("start_rule_anchor"),
            getNullableInt(rs, "start_rule_offset_days"),
            getNullableInt(rs, "start_rule_weekday"),
            rs.getString("start_rule_weekday_policy"),
            getNullableInt(rs, "start_rule_n"),
            getNullableInt(rs, "start_rule_days_before"),
            rs.getString("feast_rule_type"),
            getNullableInt(rs, "feast_rule_month"),
            getNullableInt(rs, "feast_rule_day"),
            rs.getString("feast_rule_anchor"),
            getNullableInt(rs, "feast_rule_offset_days"),
            getNullableInt(rs, "feast_rule_weekday"),
            rs.getString("feast_rule_weekday_policy"),
            getNullableInt(rs, "feast_rule_n"),
            getNullableInt(rs, "feast_rule_days_before"),
            getNullableInt(rs, "entry_duration_days"),
            getNullableInt(rs, "source_duration_days")
        );
    }

    private Integer getNullableInt(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }

    private String baseSelectSql() {
        return """
            select
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
              n.duration_days as source_duration_days
            from novena_serving_rules nsr
            join novenas n on n.id = nsr.novena_id
            """;
    }
}
