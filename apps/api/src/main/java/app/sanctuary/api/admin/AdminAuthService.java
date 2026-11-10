package app.sanctuary.api.admin;

import java.time.Instant;
import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import app.sanctuary.api.auth.service.AuthFlowException;
import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@Service
public class AdminAuthService {
    private final JdbcTemplate db;
    private final CognitoIdentityProviderClient cognito;
    private final AuthProperties auth;
    private final AdminProperties properties;
    private final AdminCrypto crypto;
    private final ObjectMapper json;
    public record Challenge(String mode, String username, String providerSession, String accessToken) {}
    public AdminAuthService(JdbcTemplate db, CognitoIdentityProviderClient cognito, AuthProperties auth,
                            AdminProperties properties, AdminCrypto crypto, ObjectMapper json) {
        this.db = db; this.cognito = cognito; this.auth = auth;
        this.properties = properties; this.crypto = crypto; this.json = json;
    }
    public void requireConfigured() {
        if (!properties.configured() || !auth.enabled() || properties.clientId() == null || properties.clientId().isBlank())
            throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE, "Admin access is not enabled.");
    }
    public Map<String, String> login(String email, String password, String remoteAddress) {
        requireConfigured();
        rateLimit("global", 100);
        rateLimit("ip:" + AdminCrypto.hash(remoteAddress), 20);
        if (!AdminProperties.OWNER_EMAIL.equalsIgnoreCase(email.strip())) throw denied();
        rateLimit("owner", 20);
        try {
            var result = cognito.initiateAuth(InitiateAuthRequest.builder().clientId(properties.clientId())
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(Map.of("USERNAME", AdminProperties.OWNER_EMAIL, "PASSWORD", password)).build());
            if (result.challengeName() == ChallengeNameType.SOFTWARE_TOKEN_MFA) {
                return saveChallenge(new Challenge("verify", result.challengeParameters()
                    .getOrDefault("USER_ID_FOR_SRP", AdminProperties.OWNER_EMAIL), result.session(), ""), "");
            }
            if (result.authenticationResult() != null) {
                String access = result.authenticationResult().accessToken();
                var user = checkedOwner(access);
                // Never allow password-only re-enrollment to replace a verified factor.
                if (user.userMFASettingList().contains("SOFTWARE_TOKEN_MFA")) throw denied();
                var setup = cognito.associateSoftwareToken(AssociateSoftwareTokenRequest.builder().accessToken(access).build());
                return saveChallenge(new Challenge("enroll", user.username(), "", access), setup.secretCode());
            }
            // For a pool requiring MFA, Cognito provides a setup session without tokens.
            if (result.challengeName() == ChallengeNameType.MFA_SETUP) {
                var setup = cognito.associateSoftwareToken(AssociateSoftwareTokenRequest.builder().session(result.session()).build());
                return saveChallenge(new Challenge("setup", result.challengeParameters()
                    .getOrDefault("USER_ID_FOR_SRP", AdminProperties.OWNER_EMAIL), setup.session(), ""), setup.secretCode());
            }
            throw new AuthFlowException(HttpStatus.UNAUTHORIZED, "Complete any required account setup through account support first.");
        } catch (CognitoIdentityProviderException exception) { throw denied(); }
    }
    private Map<String, String> saveChallenge(Challenge challenge, String secret) {
        try {
            String token = crypto.token();
            db.update("DELETE FROM admin_challenges WHERE expires_at < now()");
            db.update("INSERT INTO admin_challenges(token_hash, encrypted_state, expires_at) VALUES (?, ?, now() + interval '5 minutes')",
                AdminCrypto.hash(token), crypto.seal(json.writeValueAsString(challenge)));
            return Map.of("challenge", token, "mode", challenge.mode(), "secret", secret);
        } catch (java.io.IOException exception) { throw new IllegalStateException("Unable to create admin challenge"); }
    }
    public Map<String, String> verify(String token, String code) {
        requireConfigured();
        if (!code.matches("[0-9]{6}")) throw denied();
        // Atomic attempt increment survives invalid codes; each challenge has at most five attempts.
        List<String> states = db.query("UPDATE admin_challenges SET attempts = attempts + 1 WHERE token_hash = ? AND expires_at > now() AND attempts < 5 RETURNING encrypted_state",
            (rs, row) -> rs.getString(1), AdminCrypto.hash(token));
        if (states.isEmpty()) throw denied();
        try {
            Challenge challenge = json.readValue(crypto.open(states.getFirst()), Challenge.class);
            String access;
            if (challenge.mode().equals("enroll")) {
                checkedOwner(challenge.accessToken());
                var result = cognito.verifySoftwareToken(VerifySoftwareTokenRequest.builder()
                    .accessToken(challenge.accessToken()).userCode(code).friendlyDeviceName("Sanctuary Admin").build());
                if (result.status() != VerifySoftwareTokenResponseType.SUCCESS) throw denied();
                cognito.setUserMFAPreference(SetUserMfaPreferenceRequest.builder().accessToken(challenge.accessToken())
                    .softwareTokenMfaSettings(SoftwareTokenMfaSettingsType.builder().enabled(true).preferredMfa(true).build()).build());
                access = challenge.accessToken();
            } else {
                String session = challenge.providerSession();
                ChallengeNameType type = ChallengeNameType.SOFTWARE_TOKEN_MFA;
                Map<String, String> responses = Map.of("USERNAME", challenge.username(), "SOFTWARE_TOKEN_MFA_CODE", code);
                if (challenge.mode().equals("setup")) {
                    var result = cognito.verifySoftwareToken(VerifySoftwareTokenRequest.builder().session(session).userCode(code).build());
                    if (result.status() != VerifySoftwareTokenResponseType.SUCCESS) throw denied();
                    session = result.session(); type = ChallengeNameType.MFA_SETUP;
                    responses = Map.of("USERNAME", challenge.username());
                }
                var result = cognito.respondToAuthChallenge(RespondToAuthChallengeRequest.builder()
                    .clientId(properties.clientId()).challengeName(type).session(session).challengeResponses(responses).build());
                if (result.authenticationResult() == null) throw denied();
                access = result.authenticationResult().accessToken();
            }
            checkedOwner(access);
            // Consume once before issuing any session, including concurrent verification requests.
            if (db.update("DELETE FROM admin_challenges WHERE token_hash = ?", AdminCrypto.hash(token)) != 1) throw denied();
            String session = crypto.token();
            db.update("DELETE FROM admin_sessions WHERE expires_at < now() OR last_seen_at < now() - interval '15 minutes'");
            db.update("INSERT INTO admin_sessions(token_hash, owner_sub, expires_at) VALUES (?, ?, now() + interval '8 hours')",
                AdminCrypto.hash(session), properties.ownerSub());
            return Map.of("token", session, "email", AdminProperties.OWNER_EMAIL);
        } catch (CognitoIdentityProviderException | java.io.IOException | IllegalArgumentException exception) { throw denied(); }
    }
    private GetUserResponse checkedOwner(String access) {
        var user = cognito.getUser(GetUserRequest.builder().accessToken(access).build());
        Map<String, String> attributes = user.userAttributes().stream().collect(java.util.stream.Collectors.toMap(AttributeType::name, AttributeType::value));
        if (!properties.ownerSub().equals(attributes.get("sub"))
            || !AdminProperties.OWNER_EMAIL.equalsIgnoreCase(attributes.getOrDefault("email", ""))
            || !"true".equals(attributes.get("email_verified"))) throw denied();
        return user;
    }
    public String authenticate(String authorization) {
        requireConfigured();
        String token = bearer(authorization);
        List<String> subjects = db.query("UPDATE admin_sessions SET last_seen_at = now() WHERE token_hash = ? AND owner_sub = ? AND expires_at > now() AND last_seen_at > now() - interval '15 minutes' RETURNING owner_sub",
            (rs, row) -> rs.getString(1), AdminCrypto.hash(token), properties.ownerSub());
        if (subjects.isEmpty()) throw denied();
        return subjects.getFirst();
    }
    public void logout(String authorization) {
        db.update("DELETE FROM admin_sessions WHERE token_hash = ?", AdminCrypto.hash(bearer(authorization)));
    }
    private String bearer(String authorization) {
        if (authorization == null || !authorization.matches("Bearer [A-Za-z0-9_-]{43}")) throw denied();
        return authorization.substring(7);
    }
    private void rateLimit(String bucket, int limit) {
        Integer count = db.queryForObject("INSERT INTO admin_login_attempts(bucket, attempts, expires_at) VALUES (?, 1, now() + interval '15 minutes') ON CONFLICT (bucket) DO UPDATE SET attempts = CASE WHEN admin_login_attempts.expires_at < now() THEN 1 ELSE admin_login_attempts.attempts + 1 END, expires_at = CASE WHEN admin_login_attempts.expires_at < now() THEN now() + interval '15 minutes' ELSE admin_login_attempts.expires_at END RETURNING attempts", Integer.class, bucket);
        if (count != null && count > limit) throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many sign-in attempts. Try again in 15 minutes.");
    }
    static AuthFlowException denied() { return new AuthFlowException(HttpStatus.UNAUTHORIZED, "Admin verification failed or the session expired. Sign in again."); }
}
