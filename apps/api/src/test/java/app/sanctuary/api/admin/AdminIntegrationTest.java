package app.sanctuary.api.admin;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.sanctuary.api.config.AuthProperties;
import app.sanctuary.api.auth.service.AuthFlowException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL", matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class AdminIntegrationTest {
    static JdbcTemplate db;
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    AdminProperties properties = new AdminProperties(true, "owner-sub", "admin-client", Base64.getEncoder().encodeToString(new byte[32]));
    @BeforeAll static void migrate() {
        var source = new DriverManagerDataSource(System.getenv("ADMIN_TEST_DB_URL"), "sanctuary_test", "sanctuary_test");
        Flyway.configure().dataSource(source).load().migrate(); db = new JdbcTemplate(source);
    }
    @BeforeEach void clear() { db.execute("TRUNCATE admin_sessions, admin_challenges, admin_login_attempts, admin_content_audit"); }
    @Test void encryptedChallengesRejectTampering() {
        var crypto = new AdminCrypto(properties); String encrypted = crypto.seal("private provider token");
        assertEquals("private provider token", crypto.open(encrypted));
        byte[] bytes = Base64.getDecoder().decode(encrypted); bytes[15] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> crypto.open(Base64.getEncoder().encodeToString(bytes)));
    }
    @Test void contentPublishesTransactionallyWithAuditAndRejectsStaleEdits() {
        var service = new AdminContentService(db, json);
        String id = "admin-test-" + UUID.randomUUID();
        Map<String, Object> values = new LinkedHashMap<>();
        service.fields("prayers").forEach(f -> values.put(f.name(), f.required() ? "" : null));
        values.put("id", id); values.put("slug", id); values.put("title_en", "Test prayer"); values.put("body_en", "Test body"); values.put("category", "prayer");
        var created = service.save("owner-sub", "prayers", "", new AdminContentService.Save(values, "", "Create test content"), true);
        String version = (String) created.get("version"); values.put("body_en", "Updated prayer");
        service.save("owner-sub", "prayers", id, new AdminContentService.Save(values, version, "Correct test content"), false);
        assertEquals(2, db.queryForObject("SELECT count(*) FROM admin_content_audit WHERE record_id = ?", Integer.class, id));
        assertThrows(AuthFlowException.class, () -> service.save("owner-sub", "prayers", id, new AdminContentService.Save(values, version, "Stale content edit"), false));
        assertEquals("Updated prayer", db.queryForObject("SELECT body_en FROM prayers WHERE id = ?", String.class, id));
        assertThrows(AuthFlowException.class, () -> service.fields("users; DROP TABLE users"));
        db.update("DELETE FROM prayers WHERE id = ?", id);
    }
    @Test void passwordAloneNeverIssuesSessionAndMfaIsPinnedToOwner() {
        var cognito = mock(CognitoIdentityProviderClient.class);
        var service = auth(cognito);
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder()
            .challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA).session("provider-session").build());
        var challenge = service.login(AdminProperties.OWNER_EMAIL, "test-password", "127.0.0.1");
        assertEquals(0, db.queryForObject("SELECT count(*) FROM admin_sessions", Integer.class));
        when(cognito.respondToAuthChallenge(any(RespondToAuthChallengeRequest.class))).thenReturn(RespondToAuthChallengeResponse.builder()
            .authenticationResult(AuthenticationResultType.builder().accessToken("access").build()).build());
        when(cognito.getUser(any(GetUserRequest.class))).thenReturn(user("wrong-sub"));
        assertThrows(AuthFlowException.class, () -> service.verify(challenge.get("challenge"), "123456"));
        assertEquals(0, db.queryForObject("SELECT count(*) FROM admin_sessions", Integer.class));
        when(cognito.getUser(any(GetUserRequest.class))).thenReturn(user("owner-sub"));
        String token = service.verify(challenge.get("challenge"), "123456").get("token");
        assertEquals("owner-sub", service.authenticate("Bearer " + token));
        assertThrows(AuthFlowException.class, () -> service.verify(challenge.get("challenge"), "123456"));
        service.logout("Bearer " + token);
        assertThrows(AuthFlowException.class, () -> service.authenticate("Bearer " + token));
    }
    @Test void sessionsExpireAndOrdinaryJwtCannotAuthorize() {
        var service = auth(mock(CognitoIdentityProviderClient.class)); var crypto = new AdminCrypto(properties);
        String token = crypto.token();
        db.update("INSERT INTO admin_sessions(token_hash, owner_sub, expires_at, last_seen_at) VALUES (?, 'owner-sub', now() + interval '8 hours', now() - interval '16 minutes')", AdminCrypto.hash(token));
        assertThrows(AuthFlowException.class, () -> service.authenticate("Bearer " + token));
        assertThrows(AuthFlowException.class, () -> service.authenticate("Bearer ordinary.cognito.jwt"));
        assertThrows(AuthFlowException.class, () -> service.authenticate(null));
    }
    @Test void rejectedMfaStopsAfterFiveAttempts() {
        var cognito = mock(CognitoIdentityProviderClient.class); var service = auth(cognito);
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder().challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA).session("session").build());
        var challenge = service.login(AdminProperties.OWNER_EMAIL, "password", "127.0.0.1");
        when(cognito.respondToAuthChallenge(any(RespondToAuthChallengeRequest.class))).thenThrow(CodeMismatchException.builder().message("Wrong").build());
        for (int i = 0; i < 6; i++) assertThrows(AuthFlowException.class, () -> service.verify(challenge.get("challenge"), "000000"));
        verify(cognito, times(5)).respondToAuthChallenge(any(RespondToAuthChallengeRequest.class));
    }
    @Test void activityAndFavoritesAreScopedToEachUser() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        db.update("INSERT INTO users(id, cognito_sub, email, last_sign_in_at) VALUES (?, ?, 'first@example.org', now()), (?, ?, 'second@example.org', now())", first, first.toString(), second, second.toString());
        db.update("INSERT INTO user_favorites(user_id,item_type,item_id) VALUES (?, 'prayer', 'private-favorite')", first);
        var service = new AdminUsersService(db, mock(CognitoIdentityProviderClient.class), new AuthProperties());
        assertEquals(1, ((List<?>)service.detail(first, 0).get("favorites")).size());
        assertEquals(0, ((List<?>)service.detail(second, 0).get("favorites")).size());
        assertEquals(1, ((List<?>)service.detail(first, 0).get("events")).size());
        db.update("DELETE FROM users WHERE id IN (?, ?)", first, second);
    }
    @Test void invalidRelationshipRollsBackContentAndAudit() {
        var service = new AdminContentService(db, json);
        String id = db.queryForObject("SELECT id FROM saints ORDER BY id LIMIT 1", String.class);
        var original = service.detail("saints", id);
        @SuppressWarnings("unchecked") Map<String, Object> values = new LinkedHashMap<>((Map<String, Object>) original.get("values"));
        String oldSummary = (String) values.get("summary_en"); values.put("summary_en", "Must be rolled back");
        var request = new AdminContentService.Save(values, (String) original.get("version"), "Invalid relationship test",
            Map.of("tags", List.of(), "intentions", List.of("9223372036854775807"), "patronages", List.of()));
        var transaction = new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(db.getDataSource()));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> transaction.execute(status -> service.save("owner-sub", "saints", id, request, false)));
        assertEquals(oldSummary, db.queryForObject("SELECT summary_en FROM saints WHERE id = ?", String.class, id));
        assertEquals(0, db.queryForObject("SELECT count(*) FROM admin_content_audit", Integer.class));
    }
    @Test void realLoginRecordsAreDistinctFromProfileActivity() {
        String sub = UUID.randomUUID().toString();
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"" + sub + "\",\"email\":\"fixture@example.org\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var recorder = new UserLoginRecorder(db, json); recorder.record("header." + payload + ".signature"); recorder.record("header." + payload + ".signature");
        assertEquals(2L, db.queryForObject("SELECT login_count FROM users WHERE cognito_sub = ?", Long.class, sub));
        assertEquals(2, db.queryForObject("SELECT count(*) FROM user_activity_events e JOIN users u ON u.id = e.user_id WHERE u.cognito_sub = ? AND e.activity_type = 'login'", Integer.class, sub));
        db.update("DELETE FROM users WHERE cognito_sub = ?", sub);
    }
    @Test void newNovenaCannotPublishWithoutAllNineDays() {
        var service = new AdminContentService(db, json);
        String id = "novena-test-" + UUID.randomUUID();
        Map<String, Object> values = new LinkedHashMap<>();
        service.fields("novenas").forEach(f -> values.put(f.name(), f.required() ? "" : null));
        values.put("id", id); values.put("slug", id); values.put("title_en", "Test novena"); values.put("duration_days", 9);
        assertThrows(AuthFlowException.class, () -> service.save("owner-sub", "novenas", "", new AdminContentService.Save(values, "", "Missing day bundle"), true));
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            Map<String, Object> day = new LinkedHashMap<>();
            service.fields("novena-days").stream().filter(f -> !f.generated()).forEach(f -> day.put(f.name(), ""));
            day.put("prayer_en", "Test prayer " + i); days.add(day);
        }
        var transaction = new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(db.getDataSource()));
        transaction.execute(status -> service.save("owner-sub", "novenas", "", new AdminContentService.Save(values, "", "Publish complete novena", Map.of("tags", List.of(), "intentions", List.of()), days), true));
        assertEquals(9, db.queryForObject("SELECT count(*) FROM novena_days WHERE novena_id = ?", Integer.class, id));
        assertEquals(10, db.queryForObject("SELECT count(*) FROM admin_content_audit", Integer.class));
        db.update("DELETE FROM novenas WHERE id = ?", id);
    }
    @Test void firstEnrollmentRequiresVerifiedTotpBeforeSession() {
        var cognito = mock(CognitoIdentityProviderClient.class); var service = auth(cognito);
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder()
            .authenticationResult(AuthenticationResultType.builder().accessToken("bootstrap-access").build()).build());
        when(cognito.getUser(any(GetUserRequest.class))).thenReturn(user("owner-sub"));
        when(cognito.associateSoftwareToken(any(AssociateSoftwareTokenRequest.class))).thenReturn(AssociateSoftwareTokenResponse.builder().secretCode("synthetic-setup-secret").build());
        var challenge = service.login(AdminProperties.OWNER_EMAIL, "password", "127.0.0.1");
        assertEquals("enroll", challenge.get("mode"));
        assertEquals(0, db.queryForObject("SELECT count(*) FROM admin_sessions", Integer.class));
        when(cognito.verifySoftwareToken(any(VerifySoftwareTokenRequest.class))).thenReturn(VerifySoftwareTokenResponse.builder().status(VerifySoftwareTokenResponseType.SUCCESS).build());
        String token = service.verify(challenge.get("challenge"), "123456").get("token");
        assertEquals("owner-sub", service.authenticate("Bearer " + token));
        verify(cognito).setUserMFAPreference(any(SetUserMfaPreferenceRequest.class));
    }
    @Test void passwordOnlyResponseCannotReplaceAnExistingFactor() {
        var cognito = mock(CognitoIdentityProviderClient.class); var service = auth(cognito);
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder()
            .authenticationResult(AuthenticationResultType.builder().accessToken("access").build()).build());
        when(cognito.getUser(any(GetUserRequest.class))).thenReturn(user("owner-sub").toBuilder().userMFASettingList("SOFTWARE_TOKEN_MFA").build());
        assertThrows(AuthFlowException.class, () -> service.login(AdminProperties.OWNER_EMAIL, "password", "127.0.0.1"));
        verify(cognito, never()).associateSoftwareToken(any(AssociateSoftwareTokenRequest.class));
        assertEquals(0, db.queryForObject("SELECT count(*) FROM admin_sessions", Integer.class));
    }
    @Test void invitationPasswordSetupCannotIssueAnAdminSession() {
        var cognito = mock(CognitoIdentityProviderClient.class); var service = auth(cognito);
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder()
            .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED).session("temporary-session").build());
        var challenge = service.login(AdminProperties.OWNER_EMAIL, "temporary-password", "127.0.0.1");
        assertEquals("password", challenge.get("mode"));
        assertThrows(AuthFlowException.class, () -> service.verify(challenge.get("challenge"), "123456"));
        when(cognito.respondToAuthChallenge(any(RespondToAuthChallengeRequest.class))).thenReturn(RespondToAuthChallengeResponse.builder()
            .authenticationResult(AuthenticationResultType.builder().accessToken("new-access").build()).build());
        when(cognito.getUser(any(GetUserRequest.class))).thenReturn(user("owner-sub"));
        when(cognito.associateSoftwareToken(any(AssociateSoftwareTokenRequest.class))).thenReturn(AssociateSoftwareTokenResponse.builder().secretCode("synthetic-setup").build());
        var next = service.password(challenge.get("challenge"), "SyntheticPassword123!");
        assertEquals("enroll", next.get("mode"));
        assertEquals(0, db.queryForObject("SELECT count(*) FROM admin_sessions", Integer.class));
        assertThrows(AuthFlowException.class, () -> service.password(challenge.get("challenge"), "SyntheticPassword123!"));
    }
    private AdminAuthService auth(CognitoIdentityProviderClient cognito) {
        return new AdminAuthService(db, cognito, new AuthProperties(true, "", "pool", "public-client"), properties, new AdminCrypto(properties), json);
    }
    private GetUserResponse user(String sub) {
        return GetUserResponse.builder().username(sub).userAttributes(
            AttributeType.builder().name("sub").value(sub).build(),
            AttributeType.builder().name("email").value(AdminProperties.OWNER_EMAIL).build(),
            AttributeType.builder().name("email_verified").value("true").build()).build();
    }
}
