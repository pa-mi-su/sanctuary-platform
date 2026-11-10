package app.sanctuary.api.admin;

import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserLoginRecorder {
    private final JdbcTemplate db;
    private final ObjectMapper json;
    public UserLoginRecorder(JdbcTemplate db, ObjectMapper json) { this.db = db; this.json = json; }
    // Only called with an ID token returned directly by Cognito after successful password authentication.
    @Transactional
    public void record(String providerIdToken) {
        try {
            var claims = json.readTree(Base64.getUrlDecoder().decode(providerIdToken.split("\\.")[1]));
            String sub = claims.path("sub").asText();
            if (sub.isBlank()) return;
            var ids = db.query("INSERT INTO users(cognito_sub, email, display_name, last_sign_in_at, last_login_at, last_seen_at, login_count) SELECT ?, ?, ?, now(), now(), now(), 1 WHERE NOT EXISTS (SELECT 1 FROM deleted_user_accounts WHERE cognito_sub = ?) ON CONFLICT(cognito_sub) DO UPDATE SET last_login_at = now(), last_seen_at = now(), login_count = users.login_count + 1 RETURNING id",
                (rs, row) -> rs.getObject(1), sub, claims.path("email").asText(), claims.path("name").asText(), sub);
            if (!ids.isEmpty()) db.update("INSERT INTO user_activity_events(user_id, activity_type, resource_type, activity_date) VALUES (?, 'login', 'account', (now() AT TIME ZONE 'UTC')::date)", ids.getFirst());
        } catch (java.io.IOException | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalStateException("Invalid provider login response");
        }
    }
}
