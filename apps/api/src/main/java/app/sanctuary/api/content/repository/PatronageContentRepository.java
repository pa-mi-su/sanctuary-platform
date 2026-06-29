package app.sanctuary.api.content.repository;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.content.dto.SaintSummaryDto;
import app.sanctuary.api.content.dto.SearchTermDto;
import app.sanctuary.api.content.support.SupportedLanguage;

@Repository
public class PatronageContentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PatronageContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SearchTermDto> searchTerms(SupportedLanguage language, String query) {
        String locale = language.code();
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter + "%";
        String sql = """
                SELECT
                    cp.slug AS key,
                    cp.label_%s AS label,
                    COUNT(DISTINCT spl.saint_id) AS result_count,
                    (ARRAY_AGG(DISTINCT s.image_url ORDER BY s.image_url) FILTER (
                        WHERE s.image_url IS NOT NULL AND trim(s.image_url) <> ''
                    ))[1:3] AS image_urls
                FROM content_patronages cp
                JOIN saint_patronage_links spl ON spl.patronage_id = cp.id
                JOIN saints s ON s.id = spl.saint_id
                WHERE (? = ''
                    OR cp.label_%s ILIKE ?
                    OR EXISTS (
                        SELECT 1
                        FROM content_patronage_aliases cpa
                        WHERE cpa.patronage_id = cp.id
                          AND cpa.locale = ?
                          AND cpa.alias_text ILIKE ?
                    ))
                GROUP BY cp.slug, cp.label_%s
                ORDER BY lower(cp.label_%s), cp.label_%s
                """.formatted(locale, locale, locale, locale, locale);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new SearchTermDto(
                rs.getString("key"),
                rs.getString("label"),
                rs.getInt("result_count"),
                stringArray(rs.getArray("image_urls"))
            ),
            filter,
            likeQuery,
            locale,
            likeQuery
        );
    }

    public List<SaintSummaryDto> findSaintsByPatronage(SupportedLanguage language, String key) {
        String locale = language.code();
        String sql = """
            SELECT DISTINCT
                s.id,
                s.slug,
                s.name_%s AS name,
                s.feast_month,
                s.feast_day,
                s.feast_label_%s AS feast_label,
                s.summary_%s AS summary,
                s.image_url,
                ARRAY_AGG(DISTINCT all_patronages.label_%s ORDER BY all_patronages.label_%s) AS patronages
            FROM saints s
            JOIN saint_patronage_links spl ON spl.saint_id = s.id
            JOIN content_patronages cp ON cp.id = spl.patronage_id
            LEFT JOIN saint_patronage_links all_links ON all_links.saint_id = s.id
            LEFT JOIN content_patronages all_patronages ON all_patronages.id = all_links.patronage_id
            WHERE cp.slug = ?
            GROUP BY s.id, s.slug, s.name_%s, s.feast_month, s.feast_day, s.feast_label_%s, s.summary_%s, s.image_url
            ORDER BY s.feast_month, s.feast_day, name
            """.formatted(locale, locale, locale, locale, locale, locale, locale, locale);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                String[] patronages = (String[]) rs.getArray("patronages").getArray();
                return new SaintSummaryDto(
                    rs.getString("id"),
                    rs.getString("slug"),
                    rs.getString("name"),
                    rs.getInt("feast_month"),
                    rs.getInt("feast_day"),
                    rs.getString("feast_label"),
                    rs.getString("summary"),
                    rs.getString("image_url"),
                    Arrays.asList(patronages),
                    List.of()
                );
            },
            key
        );
    }

    private static List<String> stringArray(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return Arrays.stream((String[]) array.getArray())
            .filter(value -> value != null && !value.isBlank())
            .toList();
    }
}
