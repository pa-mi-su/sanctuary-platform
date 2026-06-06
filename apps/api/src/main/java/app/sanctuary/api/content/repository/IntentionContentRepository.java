package app.sanctuary.api.content.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.content.dto.IntentionSearchResultDto;
import app.sanctuary.api.content.dto.NovenaSummaryDto;
import app.sanctuary.api.content.dto.SaintSummaryDto;
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
            searchSaints(locale, filter, likeQuery)
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
                OR n.title_%s ILIKE ?
                OR n.slug ILIKE ?
                OR EXISTS (
                    SELECT 1
                    FROM content_intention_aliases cia
                    WHERE cia.intention_id = ci.id
                      AND cia.locale = ?
                      AND cia.alias_text ILIKE ?
                ))
            ORDER BY title
            """.formatted(locale, locale, locale, locale);

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
            likeQuery,
            likeQuery,
            locale,
            likeQuery
        );
    }

    private List<SaintSummaryDto> searchSaints(String locale, String filter, String likeQuery) {
        String sql = """
            SELECT DISTINCT
                s.id,
                s.slug,
                s.name_%s AS name,
                s.image_url,
                s.feast_month,
                s.feast_day,
                s.feast_label_%s AS feast_label,
                s.summary_%s AS summary
            FROM saints s
            JOIN saint_intention_links sil ON sil.saint_id = s.id
            JOIN content_intentions ci ON ci.id = sil.intention_id
            WHERE (? = ''
                OR ci.label_%s ILIKE ?
                OR s.name_%s ILIKE ?
                OR s.summary_%s ILIKE ?
                OR s.slug ILIKE ?
                OR EXISTS (
                    SELECT 1
                    FROM content_intention_aliases cia
                    WHERE cia.intention_id = ci.id
                      AND cia.locale = ?
                      AND cia.alias_text ILIKE ?
                ))
            ORDER BY s.feast_month, s.feast_day, name
            """.formatted(locale, locale, locale, locale, locale, locale);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new SaintSummaryDto(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getInt("feast_month"),
                rs.getInt("feast_day"),
                rs.getString("feast_label"),
                rs.getString("summary"),
                rs.getString("image_url")
            ),
            filter,
            likeQuery,
            likeQuery,
            likeQuery,
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
}
