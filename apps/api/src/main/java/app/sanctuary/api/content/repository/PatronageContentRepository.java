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

    public List<SearchTermDto> searchTerms(String query) {
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter + "%";
        return jdbcTemplate.query(
            """
                SELECT
                    regexp_replace(lower(trim(sp.patronage)), '[^a-z0-9]+', '-', 'g') AS key,
                    MIN(trim(sp.patronage)) AS label,
                    COUNT(DISTINCT sp.saint_id) AS result_count,
                    (ARRAY_AGG(DISTINCT s.image_url ORDER BY s.image_url) FILTER (
                        WHERE s.image_url IS NOT NULL AND trim(s.image_url) <> ''
                    ))[1:3] AS image_urls
                FROM saint_patronages sp
                JOIN saints s ON s.id = sp.saint_id
                WHERE trim(sp.patronage) <> ''
                  AND (? = '' OR sp.patronage ILIKE ?)
                GROUP BY regexp_replace(lower(trim(sp.patronage)), '[^a-z0-9]+', '-', 'g')
                ORDER BY MIN(lower(trim(sp.patronage)))
                """,
            (rs, rowNum) -> new SearchTermDto(
                rs.getString("key"),
                rs.getString("label"),
                rs.getInt("result_count"),
                stringArray(rs.getArray("image_urls"))
            ),
            filter,
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
                ARRAY_AGG(DISTINCT all_patronages.patronage ORDER BY all_patronages.patronage) AS patronages
            FROM saints s
            JOIN saint_patronages sp ON sp.saint_id = s.id
            LEFT JOIN saint_patronages all_patronages ON all_patronages.saint_id = s.id
            WHERE regexp_replace(lower(trim(sp.patronage)), '[^a-z0-9]+', '-', 'g') = ?
            GROUP BY s.id, s.slug, s.name_%s, s.feast_month, s.feast_day, s.feast_label_%s, s.summary_%s, s.image_url
            ORDER BY s.feast_month, s.feast_day, name
            """.formatted(locale, locale, locale, locale, locale, locale);

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
