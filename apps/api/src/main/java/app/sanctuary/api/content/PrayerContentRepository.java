package app.sanctuary.api.content;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PrayerContentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PrayerContentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PrayerSummaryResponse> list(String language, String query) {
        String locale = normalizeLanguage(language);
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
            (rs, rowNum) -> new PrayerSummaryResponse(
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

    public Optional<PrayerDetailResponse> findBySlug(String slug, String language) {
        String locale = normalizeLanguage(language);
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

        List<PrayerDetailResponse> prayers = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new PrayerDetailResponse(
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

        PrayerDetailResponse prayer = prayers.getFirst();
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

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }

        return switch (language.toLowerCase(Locale.US)) {
            case "en", "es", "pl" -> language.toLowerCase(Locale.US);
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }
}
