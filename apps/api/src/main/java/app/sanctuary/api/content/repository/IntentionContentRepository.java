package app.sanctuary.api.content.repository;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.content.dto.IntentionSearchResultDto;
import app.sanctuary.api.content.dto.NovenaSummaryDto;
import app.sanctuary.api.content.dto.SearchTermDto;
import app.sanctuary.api.content.support.SupportedLanguage;

@Repository
public class IntentionContentRepository {

    private final JdbcTemplate jdbcTemplate;

    public IntentionContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public IntentionSearchResultDto search(SupportedLanguage language, String query) {
        String locale = language.code();
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter + "%";

        return new IntentionSearchResultDto(
            searchNovenas(locale, filter, likeQuery),
            List.of()
        );
    }

    public List<SearchTermDto> searchNovenaIntentionTerms(SupportedLanguage language, String query) {
        String locale = language.code();
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter + "%";
        String sql = """
            SELECT
                ci.slug AS key,
                ci.label_%s AS label,
                COUNT(DISTINCT nil.novena_id) AS result_count,
                (ARRAY_AGG(DISTINCT n.image_url ORDER BY n.image_url) FILTER (
                    WHERE n.image_url IS NOT NULL AND trim(n.image_url) <> ''
                ))[1:3] AS image_urls
            FROM content_intentions ci
            JOIN novena_intention_links nil ON nil.intention_id = ci.id
            JOIN novenas n ON n.id = nil.novena_id
            WHERE (? = ''
                OR ci.label_%s ILIKE ?
                OR EXISTS (
                    SELECT 1
                    FROM content_intention_aliases cia
                    WHERE cia.intention_id = ci.id
                      AND cia.locale = ?
                      AND cia.alias_text ILIKE ?
                ))
            GROUP BY ci.slug, ci.label_%s
            ORDER BY ci.label_%s
            """.formatted(locale, locale, locale, locale);

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

    public List<NovenaSummaryDto> findNovenasByIntention(String locale, String key) {
        String sql = """
            SELECT DISTINCT
                n.id,
                n.slug,
                n.title_%s AS title,
                n.description_%s AS description,
                n.duration_days,
                n.image_url
            FROM novenas n
            JOIN novena_intention_links nil ON nil.novena_id = n.id
            JOIN content_intentions ci ON ci.id = nil.intention_id
            WHERE ci.slug = ?
            ORDER BY title
            """.formatted(locale, locale);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                String novenaId = rs.getString("id");
                return new NovenaSummaryDto(
                    novenaId,
                    rs.getString("slug"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getInt("duration_days"),
                    rs.getString("image_url"),
                    fetchNovenaIntentions(novenaId, locale)
                );
            },
            key
        );
    }

    private List<NovenaSummaryDto> searchNovenas(String locale, String filter, String likeQuery) {
        String sql = """
            SELECT DISTINCT
                n.id,
                n.slug,
                n.title_%s AS title,
                n.description_%s AS description,
                n.duration_days,
                n.image_url
            FROM novenas n
            JOIN novena_intention_links nil ON nil.novena_id = n.id
            JOIN content_intentions ci ON ci.id = nil.intention_id
            WHERE (? = ''
                OR ci.label_%s ILIKE ?
                OR EXISTS (
                    SELECT 1
                    FROM content_intention_aliases cia
                    WHERE cia.intention_id = ci.id
                      AND cia.locale = ?
                      AND cia.alias_text ILIKE ?
                ))
            ORDER BY title
            """.formatted(locale, locale, locale);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                String novenaId = rs.getString("id");
                return new NovenaSummaryDto(
                    novenaId,
                    rs.getString("slug"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getInt("duration_days"),
                    rs.getString("image_url"),
                    fetchNovenaIntentions(novenaId, locale)
                );
            },
            filter,
            likeQuery,
            locale,
            likeQuery
        );
    }

    private List<String> fetchNovenaIntentions(String novenaId, String locale) {
        return jdbcTemplate.query(
            """
                SELECT ci.label_%s AS intention_text
                FROM novena_intention_links nil
                JOIN content_intentions ci ON ci.id = nil.intention_id
                WHERE nil.novena_id = ?
                ORDER BY nil.sort_order, ci.id
                """.formatted(locale),
            (rs, rowNum) -> rs.getString("intention_text"),
            novenaId
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
