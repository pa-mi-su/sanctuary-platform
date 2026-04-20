package app.sanctuary.api.content.repository;

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

    public List<PrayerSummaryDto> list(SupportedLanguage language, String query) {
        String locale = language.code();
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter + "%";

        String sql = """
            SELECT
                id,
                slug,
                title_%s AS title,
                body_%s AS body_text,
                category,
                image_url
            FROM prayers
            WHERE (? = '' OR
                title_%s ILIKE ? OR
                body_%s ILIKE ? OR
                slug ILIKE ? OR
                category ILIKE ?)
            ORDER BY title_%s
            """.formatted(locale, locale, locale, locale, locale);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new PrayerSummaryDto(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("title"),
                summarizeBody(rs.getString("body_text")),
                rs.getString("category"),
                rs.getString("image_url")
            ),
            filter,
            likeQuery,
            likeQuery,
            likeQuery,
            likeQuery
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
                rs.getString("category"),
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
}
