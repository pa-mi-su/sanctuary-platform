package app.sanctuary.api.user.service;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.sanctuary.api.user.dto.ContentReminder;

@Service
public class ContentReminderService {
    private final JdbcTemplate jdbc;
    public ContentReminderService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ContentReminder> list(UUID userId) {
        return jdbc.query("SELECT kind, slug, title, hour, minute FROM user_content_reminders WHERE user_id = ? ORDER BY lower(title), kind, slug",
            (rs, n) -> new ContentReminder(rs.getString("kind"), rs.getString("slug"), rs.getString("title"), rs.getInt("hour"), rs.getInt("minute")), userId);
    }

    @Transactional
    public void save(UUID userId, ContentReminder reminder) {
        lock(userId);
        String table = switch (reminder.kind()) { case "prayers" -> "prayers"; case "novenas" -> "novenas";
            default -> throw new IllegalArgumentException("Unsupported reminder kind."); };
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM " + table + " WHERE slug = ?)", Boolean.class, reminder.slug()))) {
            throw new IllegalArgumentException("This prayer or novena no longer exists.");
        }
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM user_content_reminders WHERE user_id = ? AND NOT (kind = ? AND slug = ?)", Integer.class, userId, reminder.kind(), reminder.slug());
        if (count != null && count >= 50) throw new IllegalArgumentException("Remove an existing reminder before adding another.");
        jdbc.update("""
            INSERT INTO user_content_reminders (user_id, kind, slug, title, hour, minute) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, kind, slug) DO UPDATE SET title = EXCLUDED.title, hour = EXCLUDED.hour, minute = EXCLUDED.minute, updated_at = NOW()
            """, userId, reminder.kind(), reminder.slug(), reminder.title(), reminder.hour(), reminder.minute());
    }

    @Transactional
    public void delete(UUID userId, String kind, String slug) {
        lock(userId);
        jdbc.update("DELETE FROM user_content_reminders WHERE user_id = ? AND kind = ? AND slug = ?", userId, kind, slug);
    }

    @Transactional
    public void clear(UUID userId) {
        lock(userId);
        jdbc.update("DELETE FROM user_content_reminders WHERE user_id = ?", userId);
    }

    private void lock(UUID userId) {
        jdbc.queryForObject("SELECT id FROM users WHERE id = ? FOR UPDATE", UUID.class, userId);
    }
}
