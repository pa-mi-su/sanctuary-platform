package app.sanctuary.api.content;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.calendar.model.NovenaServingRule;
import app.sanctuary.api.calendar.model.NovenaServingWindowResult;
import app.sanctuary.api.calendar.service.NovenaServingRuleRepository;
import app.sanctuary.api.calendar.service.NovenaServingWindowResolver;
import app.sanctuary.api.content.support.SupportedLanguage;

@Repository
public class NovenaCalendarContentRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NovenaServingRuleRepository novenaServingRuleRepository;
    private final NovenaServingWindowResolver novenaServingWindowResolver;

    public NovenaCalendarContentRepository(
        JdbcTemplate jdbcTemplate,
        NovenaServingRuleRepository novenaServingRuleRepository,
        NovenaServingWindowResolver novenaServingWindowResolver
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.novenaServingRuleRepository = novenaServingRuleRepository;
        this.novenaServingWindowResolver = novenaServingWindowResolver;
    }

    public List<NovenaSummaryResponse> list(SupportedLanguage language, String query) {
        String locale = language.code();
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter + "%";

        String sql = """
            SELECT
                id,
                slug,
                title_%s AS title,
                description_%s AS description,
                duration_days,
                image_url
            FROM novenas
            WHERE (? = '' OR
                title_%s ILIKE ? OR
                description_%s ILIKE ? OR
                slug ILIKE ?)
            ORDER BY title_%s
            """.formatted(locale, locale, locale, locale, locale);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new NovenaSummaryResponse(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("duration_days"),
                rs.getString("image_url")
            ),
            filter,
            likeQuery,
            likeQuery,
            likeQuery
        );
    }

    public List<NovenaSummaryResponse> listByIntentions(SupportedLanguage language, String query) {
        String locale = language.code();
        String filter = query == null ? "" : query.trim();
        String likeQuery = "%" + filter + "%";

        String sql = """
            SELECT DISTINCT
                n.id,
                n.slug,
                n.title_%s AS title,
                n.description_%s AS description,
                n.duration_days,
                n.image_url
            FROM novenas n
            JOIN novena_intentions ni ON ni.novena_id = n.id
            WHERE ni.locale = ?
              AND (? = '' OR ni.intention_text ILIKE ? OR n.title_%s ILIKE ? OR n.slug ILIKE ?)
            ORDER BY title
            """.formatted(locale, locale, locale);

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new NovenaSummaryResponse(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("duration_days"),
                rs.getString("image_url")
            ),
            locale,
            filter,
            likeQuery,
            likeQuery,
            likeQuery
        );
    }

    public List<NovenaCalendarDateResponse> calendarRange(LocalDate start, LocalDate end, SupportedLanguage language) {
        String locale = language.code();
        Map<LocalDate, Map<String, NovenaSummaryResponse>> byDate = new LinkedHashMap<>();
        Map<LocalDate, NovenaSummaryResponse> startingByDate = new LinkedHashMap<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            byDate.put(cursor, new LinkedHashMap<>());
            cursor = cursor.plusDays(1);
        }

        List<NovenaServingRule> rules = novenaServingRuleRepository.findAll();
        for (NovenaServingRule rule : rules) {
            for (int year = start.getYear() - 1; year <= end.getYear() + 1; year++) {
                NovenaServingWindowResult window;
                try {
                    window = novenaServingWindowResolver.resolve(rule, year);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                LocalDate intersectionStart = window.startDate().isAfter(start) ? window.startDate() : start;
                LocalDate intersectionEnd = window.endDate().isBefore(end) ? window.endDate() : end;

                if (intersectionEnd.isBefore(intersectionStart)) {
                    continue;
                }

                NovenaSummaryResponse summary = fetchNovenaSummary(rule.novenaId(), locale);
                if (summary == null) {
                    continue;
                }

                if (!window.startDate().isBefore(start) && !window.startDate().isAfter(end)) {
                    startingByDate.putIfAbsent(window.startDate(), summary);
                }

                LocalDate day = intersectionStart;
                while (!day.isAfter(intersectionEnd)) {
                    byDate.get(day).putIfAbsent(summary.id(), summary);
                    day = day.plusDays(1);
                }
            }
        }

        List<NovenaCalendarDateResponse> response = new ArrayList<>();
        for (Map.Entry<LocalDate, Map<String, NovenaSummaryResponse>> entry : byDate.entrySet()) {
            response.add(new NovenaCalendarDateResponse(
                entry.getKey(),
                new ArrayList<>(entry.getValue().values()),
                startingByDate.get(entry.getKey())
            ));
        }
        return response;
    }

    public Optional<NovenaDetailResponse> findBySlug(String slug, SupportedLanguage language) {
        String locale = language.code();
        String sql = """
            SELECT
                id,
                slug,
                title_%s AS title,
                description_%s AS description,
                duration_days,
                image_url
            FROM novenas
            WHERE slug = ?
            """.formatted(locale, locale);

        List<NovenaDetailResponse> novenas = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new NovenaDetailResponse(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("duration_days"),
                rs.getString("image_url"),
                List.of(),
                List.of(),
                List.of()
            ),
            slug
        );

        if (novenas.isEmpty()) {
            return Optional.empty();
        }

        NovenaDetailResponse novena = novenas.getFirst();
        List<String> tags = jdbcTemplate.query(
            """
                SELECT tag
                FROM novena_tags
                WHERE novena_id = ?
                ORDER BY tag
                """,
            (rs, rowNum) -> rs.getString("tag"),
            novena.id()
        );
        List<String> intentions = jdbcTemplate.query(
            """
                SELECT intention_text
                FROM novena_intentions
                WHERE novena_id = ? AND locale = ?
                ORDER BY sort_order, id
                """,
            (rs, rowNum) -> rs.getString("intention_text"),
            novena.id(),
            locale
        );
        List<NovenaDayDetailResponse> days = jdbcTemplate.query(
            """
                SELECT
                    day_number,
                    title_%s AS title,
                    scripture_%s AS scripture,
                    prayer_%s AS prayer,
                    reflection_%s AS reflection,
                    body_%s AS body_text
                FROM novena_days
                WHERE novena_id = ?
                ORDER BY day_number
                """.formatted(locale, locale, locale, locale, locale),
            (rs, rowNum) -> new NovenaDayDetailResponse(
                rs.getInt("day_number"),
                rs.getString("title"),
                rs.getString("scripture"),
                rs.getString("prayer"),
                rs.getString("reflection"),
                rs.getString("body_text")
            ),
            novena.id()
        );

        return Optional.of(novena.withTags(tags).withIntentions(intentions).withDays(days));
    }

    private NovenaSummaryResponse fetchNovenaSummary(String novenaId, String locale) {
        String sql = """
            SELECT
                id,
                slug,
                title_%s AS title,
                description_%s AS description,
                duration_days,
                image_url
            FROM novenas
            WHERE id = ?
            """.formatted(locale, locale);

        List<NovenaSummaryResponse> results = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> new NovenaSummaryResponse(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("duration_days"),
                rs.getString("image_url")
            ),
            novenaId
        );

        return results.isEmpty() ? null : results.getFirst();
    }
}
