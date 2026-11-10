package app.sanctuary.api.admin;

import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import app.sanctuary.api.auth.service.AuthFlowException;
import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@Service
public class AdminDirectory {
    private final JdbcTemplate db;
    private final CognitoIdentityProviderClient cognito;
    private final AuthProperties auth;
    public AdminDirectory(JdbcTemplate db, CognitoIdentityProviderClient cognito, AuthProperties auth) { this.db = db; this.cognito = cognito; this.auth = auth; }
    private static final String BASE = """
        FROM admin_user_directory d FULL JOIN users u ON u.cognito_sub = d.cognito_sub
        """;
    @Transactional
    public Map<String, Object> list(String query, int page, String status, String activity, boolean refresh) {
        if (query.length() > 200 || page < 0 || page > 10000) throw AdminContentService.bad("Invalid search or page.");
        String state = switch (status) {
            case "all" -> "true"; case "confirmed" -> "d.status = 'CONFIRMED'";
            case "unconfirmed" -> "d.status = 'UNCONFIRMED'"; case "disabled" -> "d.enabled = false";
            case "profile-only" -> "d.cognito_sub IS NULL"; default -> throw AdminContentService.bad("Unknown account status.");
        };
        String active = switch (activity) {
            case "all" -> "true"; case "active" -> "u.last_seen_at > now() - interval '7 days'";
            case "never" -> "u.id IS NULL OR u.last_seen_at IS NULL";
            case "saved" -> "EXISTS (SELECT 1 FROM user_favorites f WHERE f.user_id = u.id)";
            case "completed" -> "EXISTS (SELECT 1 FROM user_novena_commitments n WHERE n.user_id = u.id AND n.status = 'completed')";
            default -> throw AdminContentService.bad("Unknown activity filter.");
        };
        synchronize(refresh);
        String match = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        var rows = db.queryForList("""
            SELECT COALESCE(d.cognito_sub, u.cognito_sub) AS id, u.id::text AS profile_id,
            COALESCE(d.email, u.email) AS email, COALESCE(d.display_name, u.display_name) AS display_name,
            d.registered_at, d.email_verified, COALESCE(d.status, 'PROFILE_ONLY') AS status, d.enabled,
            u.created_at AS profile_created_at, u.last_seen_at, u.last_login_at, COALESCE(u.login_count, 0) AS login_count,
            (SELECT count(*) FROM user_favorites f WHERE f.user_id = u.id) AS saved,
            (SELECT count(*) FROM user_novena_commitments n WHERE n.user_id = u.id AND n.status = 'active') AS active_novenas,
            (SELECT count(*) FROM user_novena_commitments n WHERE n.user_id = u.id AND n.status = 'completed') AS completed_novenas
            """ + BASE + " WHERE (COALESCE(d.email, u.email, '') ILIKE ? OR COALESCE(d.display_name, u.display_name, '') ILIKE ?) AND (" + state + ") AND (" + active + ") ORDER BY d.registered_at DESC NULLS LAST, COALESCE(d.cognito_sub, u.cognito_sub) LIMIT 21 OFFSET ?", match, match, page * 20);
        var summary = db.queryForMap("""
            SELECT count(d.cognito_sub) AS registrations,
            count(*) FILTER (WHERE d.email_verified) AS verified,
            count(*) FILTER (WHERE d.status = 'UNCONFIRMED') AS unconfirmed,
            count(u.id) AS profiles,
            count(*) FILTER (WHERE u.last_seen_at > now() - interval '1 day') AS active_24h,
            count(*) FILTER (WHERE u.last_seen_at > now() - interval '7 days') AS active_7d,
            count(*) FILTER (WHERE u.last_seen_at > now() - interval '30 days') AS active_30d,
            (SELECT count(*) FROM user_novena_commitments WHERE status = 'active') AS active_novenas,
            (SELECT count(*) FROM user_novena_commitments WHERE status = 'completed') AS completed_novenas
            """ + BASE);
        OffsetDateTime synced = db.queryForObject("SELECT last_synced_at FROM admin_directory_state WHERE singleton", OffsetDateTime.class);
        return Map.of("items", rows.stream().limit(20).toList(), "hasMore", rows.size() > 20, "summary", summary, "syncedAt", synced.toInstant().toString());
    }
    private void synchronize(boolean force) {
        db.queryForObject("SELECT pg_advisory_xact_lock(724281038)", Object.class);
        Boolean fresh = db.queryForObject("SELECT last_synced_at > now() - interval '5 minutes' FROM admin_directory_state WHERE singleton", Boolean.class);
        if (!force && Boolean.TRUE.equals(fresh)) return;
        if (auth.userPoolId() == null || auth.userPoolId().isBlank()) throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE, "The account directory is not configured.");
        List<UserType> users = new ArrayList<>(); String cursor = null; int pages = 0;
        do {
            if (++pages > 1000) throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE, "Directory synchronization exceeded its limit; the previous snapshot was retained.");
            var request = ListUsersRequest.builder().userPoolId(auth.userPoolId()).limit(60);
            if (cursor != null) request.paginationToken(cursor);
            var response = cognito.listUsers(request.build()); users.addAll(response.users()); cursor = response.paginationToken();
        } while (cursor != null && !cursor.isBlank());
        UUID sync = UUID.randomUUID();
        List<Object[]> parameters = new ArrayList<>();
        for (var user : users) {
            Map<String, String> attributes = user.attributes().stream().collect(java.util.stream.Collectors.toMap(AttributeType::name, AttributeType::value));
            String sub = attributes.get("sub");
            if (sub == null || user.userCreateDate() == null) throw new AuthFlowException(HttpStatus.BAD_GATEWAY, "The account provider returned an incomplete directory. The previous snapshot was retained.");
            parameters.add(new Object[]{sub, attributes.get("email"), attributes.get("name"), "true".equals(attributes.get("email_verified")), user.userStatusAsString(), user.enabled(), java.sql.Timestamp.from(user.userCreateDate()), user.userLastModifiedDate() == null ? null : java.sql.Timestamp.from(user.userLastModifiedDate()), sync});
        }
        db.batchUpdate("""
            INSERT INTO admin_user_directory(cognito_sub, email, display_name, email_verified, status, enabled, registered_at, provider_updated_at, sync_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(cognito_sub) DO UPDATE SET email=excluded.email,
            display_name=excluded.display_name, email_verified=excluded.email_verified, status=excluded.status,
            enabled=excluded.enabled, registered_at=excluded.registered_at, provider_updated_at=excluded.provider_updated_at, sync_id=excluded.sync_id
            """, parameters);
        db.update("DELETE FROM admin_user_directory WHERE sync_id <> ?", sync);
        db.update("UPDATE admin_directory_state SET last_synced_at = now() WHERE singleton");
    }
}
