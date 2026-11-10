package app.sanctuary.api.admin;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@Service
public class AdminUsersService {
    private final JdbcTemplate db;
    private final CognitoIdentityProviderClient cognito;
    private final AuthProperties auth;
    public AdminUsersService(JdbcTemplate db, CognitoIdentityProviderClient cognito, AuthProperties auth) { this.db = db; this.cognito = cognito; this.auth = auth; }
    public Map<String, Object> summary() {
        return db.queryForMap("SELECT count(*) AS profiles, count(*) FILTER (WHERE last_seen_at > now() - interval '1 day') AS active_24h, count(*) FILTER (WHERE last_seen_at > now() - interval '7 days') AS active_7d, count(*) FILTER (WHERE last_seen_at > now() - interval '30 days') AS active_30d, (SELECT count(*) FROM user_novena_commitments WHERE status = 'active') AS active_novenas, (SELECT count(*) FROM user_novena_commitments WHERE status = 'completed') AS completed_novenas FROM users");
    }
    public Map<String, Object> list(String query, int page, boolean active) {
        String match = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        var rows = db.queryForList("SELECT u.id, u.email, u.display_name, u.created_at, u.last_login_at, u.last_seen_at, u.login_count, (SELECT count(*) FROM user_favorites f WHERE f.user_id = u.id) AS saved, (SELECT count(*) FROM user_novena_commitments n WHERE n.user_id = u.id AND n.status = 'active') AS active_novenas, (SELECT count(*) FROM user_novena_commitments n WHERE n.user_id = u.id AND n.status = 'completed') AS completed_novenas FROM users u WHERE (COALESCE(u.email, '') ILIKE ? OR COALESCE(u.display_name, '') ILIKE ?) AND (NOT ? OR u.last_seen_at > now() - interval '7 days') ORDER BY u.last_seen_at DESC NULLS LAST, u.id LIMIT 21 OFFSET ?", match, match, active, page * 20);
        return Map.of("items", rows.stream().limit(20).toList(), "hasMore", rows.size() > 20);
    }
    public Map<String, Object> detail(UUID id, int page) {
        var profiles = db.queryForList("SELECT id, email, display_name, first_name, last_name, preferred_language, created_at, last_login_at, last_seen_at, login_count FROM users WHERE id = ?", id);
        if (profiles.isEmpty()) throw new app.sanctuary.api.auth.service.AuthFlowException(org.springframework.http.HttpStatus.NOT_FOUND, "Profile not found.");
        var favorites = db.queryForList("SELECT f.item_type, f.item_id, f.created_at, COALESCE(s.name_en, n.title_en, p.title_en, f.item_id) AS title FROM user_favorites f LEFT JOIN saints s ON f.item_type = 'saint' AND f.item_id = s.id LEFT JOIN novenas n ON f.item_type = 'novena' AND f.item_id = n.id LEFT JOIN prayers p ON f.item_type = 'prayer' AND f.item_id = p.id WHERE f.user_id = ? ORDER BY f.created_at DESC LIMIT 21 OFFSET ?", id, page * 20);
        var novenas = db.queryForList("SELECT n.novena_id, c.title_en AS title, n.started_at, n.current_day, array_to_json(n.completed_days)::text AS completed_days, n.status, n.updated_at FROM user_novena_commitments n JOIN novenas c ON c.id = n.novena_id WHERE n.user_id = ? ORDER BY n.updated_at DESC LIMIT 21 OFFSET ?", id, page * 20);
        var events = db.queryForList("SELECT activity_type, resource_type, resource_id, occurred_at, metadata::text FROM user_activity_events WHERE user_id = ? ORDER BY occurred_at DESC, id DESC LIMIT 21 OFFSET ?", id, page * 20);
        return Map.of("profile", profiles.getFirst(), "favorites", favorites.stream().limit(20).toList(), "novenas", novenas.stream().limit(20).toList(), "events", events.stream().limit(20).toList(), "hasMore", favorites.size() > 20 || novenas.size() > 20 || events.size() > 20);
    }
    // Cognito includes registrations that never opened an authenticated profile endpoint.
    public Map<String, Object> registrations(String cursor) {
        if (auth.userPoolId() == null || auth.userPoolId().isBlank()) throw AdminContentService.bad("The Cognito user pool is not configured.");
        var request = ListUsersRequest.builder().userPoolId(auth.userPoolId()).limit(20);
        if (!cursor.isBlank()) request.paginationToken(cursor);
        var result = cognito.listUsers(request.build());
        List<Map<String, Object>> items = new ArrayList<>();
        for (var user : result.users()) {
            Map<String, String> attrs = user.attributes().stream().collect(java.util.stream.Collectors.toMap(AttributeType::name, AttributeType::value));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("email", attrs.getOrDefault("email", "")); row.put("name", attrs.getOrDefault("name", ""));
            row.put("registeredAt", user.userCreateDate().toString()); row.put("status", user.userStatusAsString()); row.put("enabled", user.enabled());
            var ids = db.query("SELECT id::text FROM users WHERE cognito_sub = ?", (rs, n) -> rs.getString(1), attrs.getOrDefault("sub", ""));
            row.put("profileId", ids.isEmpty() ? "" : ids.getFirst()); items.add(row);
        }
        return Map.of("items", items, "cursor", result.paginationToken() == null ? "" : result.paginationToken());
    }
}
