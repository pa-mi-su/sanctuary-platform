package app.sanctuary.api.content.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.content.dto.PrayerDetailDto;
import app.sanctuary.api.content.dto.PrayerSummaryDto;
import app.sanctuary.api.content.support.SupportedLanguage;

@Repository
public class PrayerContentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PrayerContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PrayerSummaryDto> list(SupportedLanguage language, String query, String category, String excludeCategory) {
        String locale = language.code();
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter + "%";
        String categoryFilter = category == null ? "" : category.trim();
        String excludedCategoryFilter = excludeCategory == null ? "" : excludeCategory.trim();

        String sql = """
            SELECT
                id,
                slug,
                title_%s AS title,
                body_%s AS body_text,
                note_%s AS note_text,
                category,
                image_url
            FROM prayers
            WHERE (? = '' OR
                title_%s ILIKE ? OR
                body_%s ILIKE ? OR
                slug ILIKE ? OR
                category ILIKE ?)
                AND (? = '' OR category = ?)
                AND (? = '' OR category <> ?)
            ORDER BY
                CASE
                    WHEN category = 'rosary' AND slug = 'how_to_pray_the_rosary' THEN 0
                    ELSE 1
                END,
                title_%s
            """.formatted(locale, locale, locale, locale, locale, locale);

        List<Object> params = new ArrayList<>();
        params.add(filter);
        params.add(likeQuery);
        params.add(likeQuery);
        params.add(likeQuery);
        params.add(likeQuery);
        params.add(categoryFilter);
        params.add(categoryFilter);
        params.add(excludedCategoryFilter);
        params.add(excludedCategoryFilter);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new PrayerSummaryDto(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("title"),
                summarizePreview(rs.getString("category"), rs.getString("note_text"), rs.getString("body_text")),
                displayCategory(rs.getString("category")),
                rs.getString("image_url")
            ),
            params.toArray()
        );
    }

    public Optional<PrayerDetailDto> findBySlug(String slug, SupportedLanguage language) {
        String locale = language.code();
        String sql = """
            SELECT
                id,
                slug,
                title_%s AS title,
                alternate_title_%s AS alternate_title,
                body_%s AS body_text,
                note_%s AS note_text,
                category,
                image_url,
                source_title,
                source_type
            FROM prayers
            WHERE slug = ?
            """.formatted(locale, locale, locale, locale);

        List<PrayerDetailDto> prayers = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new PrayerDetailDto(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("alternate_title"),
                rs.getString("body_text"),
                rs.getString("note_text"),
                displayCategory(rs.getString("category")),
                rs.getString("image_url"),
                rs.getString("source_title"),
                rs.getString("source_type"),
                List.of()
            ),
            slug
        );

        if (prayers.isEmpty()) {
            return Optional.empty();
        }

        PrayerDetailDto prayer = prayers.getFirst();
        List<String> tags = jdbcTemplate.query(
            """
                SELECT tag
                FROM prayer_tags
                WHERE prayer_id = ?
                ORDER BY tag
                """,
            (rs, rowNum) -> rs.getString("tag"),
            prayer.id()
        );

        return Optional.of(prayer.withTags(tags));
    }

    private String summarizePreview(String category, String note, String body) {
        if ("rosary".equalsIgnoreCase(category) && note != null && !note.isBlank()) {
            return summarizeBody(note);
        }
        return summarizeBody(body);
    }

    private String summarizeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }

        String normalized = body.replace("\r", "").trim();
        int newlineIndex = normalized.indexOf('\n');
        if (newlineIndex > 0) {
            return normalized.substring(0, newlineIndex).trim();
        }

        return normalized.length() > 160 ? normalized.substring(0, 157).trim() + "..." : normalized;
    }

    private String displayCategory(String category) {
        if (category == null || "user_provided".equalsIgnoreCase(category.trim())) {
            return "";
        }
        return category;
    }
}
