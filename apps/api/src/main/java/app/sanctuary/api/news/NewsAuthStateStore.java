package app.sanctuary.api.news;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.sanctuary.api.config.AuthProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Atomic, short-lived MFA state shared by replicas, encrypted separately from the database. */
@Repository
public class NewsAuthStateStore {
    public record Pending(String username, String session, String accessToken, String idToken,
                          Instant expires, int attempts) {
        @Override public String toString() { return "Pending[redacted]"; }
    }
    private final JdbcTemplate db;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public NewsAuthStateStore(JdbcTemplate db, @Value("${sanctuary.news.challenge-key:}") String encodedKey,
                             Environment environment, AuthProperties auth) {
        this.db = db;
        byte[] bytes;
        if (encodedKey.isBlank()) {
            if (auth.enabled() && !environment.acceptsProfiles(Profiles.of("local", "test")))
                throw new IllegalStateException("SANCTUARY_NEWS_CHALLENGE_KEY must be a 32-byte base64 or 64-character hex secret");
            // Local-only fallback: never write a generated key to source, logs, or a response.
            bytes = new byte[32]; random.nextBytes(bytes);
        } else {
            try { bytes = encodedKey.matches("[0-9a-fA-F]{64}")
                ? HexFormat.of().parseHex(encodedKey) : Base64.getDecoder().decode(encodedKey); }
            catch (IllegalArgumentException exception) { throw new IllegalStateException("Invalid news challenge encryption key"); }
            if (bytes.length != 32) throw new IllegalStateException("News challenge encryption key must contain 32 bytes");
        }
        key = new SecretKeySpec(bytes, "AES");
    }

    @Transactional
    public void put(String id, Pending state) {
        UUID identifier = UUID.fromString(id);
        db.execute("LOCK TABLE news_auth_challenges IN SHARE ROW EXCLUSIVE MODE");
        db.update("DELETE FROM news_auth_challenges WHERE expires_at <= now()");
        Integer count = db.queryForObject("SELECT count(*) FROM news_auth_challenges", Integer.class);
        if (count == null || count >= 1000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
        db.update("INSERT INTO news_auth_challenges(id,payload,expires_at) VALUES (?,?,?)",
            identifier, encrypt(identifier, state), java.time.OffsetDateTime.ofInstant(state.expires(), java.time.ZoneOffset.UTC));
    }

    public Pending take(String id) {
        UUID identifier;
        try { identifier = UUID.fromString(id); } catch (IllegalArgumentException exception) { return null; }
        var rows = db.query("DELETE FROM news_auth_challenges WHERE id=? RETURNING payload,expires_at",
            (rs, row) -> rs.getTimestamp("expires_at").toInstant().isAfter(Instant.now())
                ? decrypt(identifier, rs.getBytes("payload")) : null, identifier);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    /** Per-account limit survives replicas and cannot be bypassed by changing IP headers. */
    @Transactional
    public boolean allowLogin(String email) {
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
        db.execute("LOCK TABLE news_auth_attempts IN SHARE ROW EXCLUSIVE MODE");
        db.update("DELETE FROM news_auth_attempts WHERE expires_at <= now()");
        Integer count = db.queryForObject("SELECT count(*) FROM news_auth_attempts", Integer.class);
        if (count == null || count >= 10000) return false;
        Integer attempts = db.queryForObject("""
            INSERT INTO news_auth_attempts(key_hash,attempts,expires_at) VALUES (?,1,now()+interval '15 minutes')
            ON CONFLICT(key_hash) DO UPDATE SET attempts=LEAST(news_auth_attempts.attempts+1,21)
            RETURNING attempts
            """, Integer.class, hash);
        return attempts != null && attempts <= 20;
    }

    private byte[] encrypt(UUID id, Pending state) {
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(id.toString().getBytes(StandardCharsets.UTF_8));
            byte[] payload = cipher.doFinal(json.writeValueAsBytes(state));
            return ByteBuffer.allocate(nonce.length + payload.length).put(nonce).put(payload).array();
        } catch (Exception exception) { throw new IllegalStateException("Cannot protect sign-in state"); }
    }

    private Pending decrypt(UUID id, byte[] payload) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Arrays.copyOf(payload, 12)));
            cipher.updateAAD(id.toString().getBytes(StandardCharsets.UTF_8));
            return json.readValue(cipher.doFinal(Arrays.copyOfRange(payload, 12, payload.length)), Pending.class);
        } catch (Exception exception) {
            // Corrupted state, a rotated key, or an authentication failure consumes the challenge.
            return null;
        }
    }
}
