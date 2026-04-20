package app.sanctuary.api.content;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.content.support.SupportedLanguage;

@Repository
public class SaintContentRepository {

    private final JdbcTemplate jdbcTemplate;

    public SaintContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SaintSummaryResponse> findByFeastDay(int month, int day, SupportedLanguage language) {
        String locale = language.code();
        String sql = """
            SELECT
                id,
                slug,
                name,
                image_url,
                feast_month,
                feast_day,
                feast_label_%s AS feast_label,
                summary_%s AS summary
            FROM saints
            WHERE feast_month = ? AND feast_day = ?
            ORDER BY name
            """.formatted(locale, locale);

        return jdbcTemplate.query(sql, saintSummaryMapper(), month, day);
    }

    public List<SaintSummaryResponse> list(SupportedLanguage language, String query) {
        String locale = language.code();
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter.toLowerCase() + "%";
        String sql = """
            SELECT
                id,
                slug,
                name,
                image_url,
                feast_month,
                feast_day,
                feast_label_%s AS feast_label,
                summary_%s AS summary
            FROM saints
            WHERE (? = ''
                OR LOWER(name) LIKE ?
                OR LOWER(summary_%s) LIKE ?)
            ORDER BY feast_month, feast_day, name
            """.formatted(locale, locale, locale);

        return jdbcTemplate.query(sql, saintSummaryMapper(), filter, likeQuery, likeQuery);
    }

    public Optional<SaintDetailResponse> findBySlug(String slug, SupportedLanguage language) {
        String locale = language.code();
        String sql = """
            SELECT
                id,
                slug,
                name,
                image_url,
                feast_month,
                feast_day,
                feast_label_%s AS feast_label,
                summary_%s AS summary,
                biography_%s AS biography
            FROM saints
            WHERE slug = ?
            """.formatted(locale, locale, locale);

        List<SaintDetailResponse> saints = jdbcTemplate.query(sql, saintDetailMapper(), slug);
        if (saints.isEmpty()) {
            return Optional.empty();
        }

        SaintDetailResponse saint = saints.getFirst();
        List<SaintSourceResponse> sources = jdbcTemplate.query(
            """
                SELECT source_text, source_url
                FROM saint_sources
                WHERE saint_id = ?
                ORDER BY sort_order, id
                """,
            (rs, rowNum) -> new SaintSourceResponse(
                rs.getString("source_text"),
                rs.getString("source_url")
            ),
            saint.id()
        );

        return Optional.of(saint.withSources(sources));
    }

    private RowMapper<SaintSummaryResponse> saintSummaryMapper() {
        return (rs, rowNum) -> new SaintSummaryResponse(
            rs.getString("id"),
            rs.getString("slug"),
            rs.getString("name"),
            rs.getInt("feast_month"),
            rs.getInt("feast_day"),
            rs.getString("feast_label"),
            rs.getString("summary"),
            rs.getString("image_url")
        );
    }

    private RowMapper<SaintDetailResponse> saintDetailMapper() {
        return (rs, rowNum) -> new SaintDetailResponse(
            rs.getString("id"),
            rs.getString("slug"),
            rs.getString("name"),
            rs.getInt("feast_month"),
            rs.getInt("feast_day"),
            rs.getString("feast_label"),
            rs.getString("summary"),
            rs.getString("biography"),
            rs.getString("image_url"),
            List.of()
        );
    }
}
