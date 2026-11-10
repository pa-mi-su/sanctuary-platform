package app.sanctuary.api.admin;

import java.util.*;
import java.time.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.MockMultipartFile;
import app.sanctuary.api.auth.service.AuthFlowException;
import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL", matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class AdminUsabilityIntegrationTest {
    static JdbcTemplate db; static TransactionTemplate transaction;
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    @BeforeAll static void migrate() {
        var source = new DriverManagerDataSource(System.getenv("ADMIN_TEST_DB_URL"), "sanctuary_test", "sanctuary_test");
        Flyway.configure().dataSource(source).load().migrate(); db = new JdbcTemplate(source);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
    }
    @BeforeEach void clear() {
        db.execute("TRUNCATE admin_sessions, admin_content_audit, admin_content_images, admin_user_directory");
        db.update("UPDATE admin_directory_state SET last_synced_at = NULL WHERE singleton");
    }
    @Test void sharedTermCanBeCreatedRenamedAndUnlinkedWithoutDeletingItElsewhere() {
        var content = new AdminContentService(db, json); var relations = new AdminRelations(db);
        String label = "Teachers " + UUID.randomUUID();
        var created = transaction.execute(status -> content.createTerm("owner", "intentions", Map.of("label_en", label, "label_es", "Docentes", "label_pl", "Nauczyciele"), "Create reusable term"));
        @SuppressWarnings("unchecked") Map<String, Object> values = new LinkedHashMap<>((Map<String, Object>) created.get("values"));
        String termId = values.get("id").toString(), saint = db.queryForObject("SELECT id FROM saints ORDER BY id LIMIT 1", String.class);
        String novena = "test-novena-" + UUID.randomUUID();
        db.update("INSERT INTO novenas(id,slug,title_en,title_es,title_pl,description_en,description_es,description_pl,duration_days) VALUES (?,?, 'Fixture','Fixture','Fixture','','','',9)", novena, novena);
        db.update("INSERT INTO saint_intention_links(saint_id,intention_id) VALUES (?,?::bigint)", saint, termId);
        db.update("INSERT INTO novena_intention_links(novena_id,intention_id) VALUES (?,?::bigint)", novena, termId);
        assertThrows(AuthFlowException.class, () -> transaction.execute(status -> content.createTerm("owner", "intentions", Map.of("label_en", "  " + label.toUpperCase() + "  "), "Duplicate term test")));
        values.put("label_en", label + " corrected");
        transaction.execute(status -> content.save("owner", "intentions", termId, new AdminContentService.Save(values, (String) created.get("version"), "Correct shared spelling", Map.of()), false));
        assertTrue(json.valueToTree(relations.selected("saints", saint)).toString().contains(label + " corrected"));
        assertTrue(json.valueToTree(relations.selected("novenas", novena)).toString().contains(label + " corrected"));
        db.update("DELETE FROM saint_intention_links WHERE saint_id=? AND intention_id=?::bigint", saint, termId);
        var patronage = transaction.execute(status -> content.createTerm("owner", "patronages", Map.of("label_en", "Artists " + UUID.randomUUID(), "label_es", "Artistas", "label_pl", "Artyści"), "Create reusable patronage"));
        String patronageId = ((Map<?,?>) patronage.get("values")).get("id").toString();
        transaction.executeWithoutResult(status -> relations.save("novenas", novena, Map.of("patronages", List.of(patronageId))));
        assertTrue(json.valueToTree(relations.selected("novenas", novena)).toString().contains(patronageId));
        assertEquals(1, db.queryForObject("SELECT count(*) FROM novena_intention_links WHERE novena_id=?", Integer.class, novena));
        transaction.executeWithoutResult(status -> relations.save("novenas", novena, Map.of("patronages", List.of())));
        assertEquals(1, db.queryForObject("SELECT count(*) FROM content_patronages WHERE id::text=?", Integer.class, patronageId));
        db.update("DELETE FROM content_patronages WHERE id::text=?", patronageId);
        assertEquals(1, db.queryForObject("SELECT count(*) FROM content_intentions WHERE id::text=?", Integer.class, termId));
        assertEquals(1, db.queryForObject("SELECT count(*) FROM novena_intention_links WHERE novena_id=? AND intention_id::text=?", Integer.class, novena, termId));
        assertEquals(label + " corrected", db.queryForObject("SELECT intention_text FROM novena_intentions WHERE novena_id=? AND locale='en'", String.class, novena));
        db.update("DELETE FROM novenas WHERE id=?", novena); db.update("DELETE FROM content_intentions WHERE id::text=?", termId);
    }
    @Test void uploadDecodesAndReencodesPixelsAndRejectsSpoofedOrOversizedImages() throws Exception {
        var images = new AdminImages(db); byte[] pixels = png(20, 12);
        var uploaded = transaction.execute(status -> images.upload("owner", new MockMultipartFile("image", "portrait.png", "image/png", pixels)));
        UUID id = (UUID) uploaded.get("id"); var stored = images.get(id);
        assertEquals("image/png", stored.type()); assertEquals(20, ImageIO.read(new ByteArrayInputStream(stored.data())).getWidth());
        assertEquals(1, db.queryForObject("SELECT count(*) FROM admin_content_audit WHERE entity='images'", Integer.class));
        assertThrows(AuthFlowException.class, () -> images.upload("owner", new MockMultipartFile("image", "spoof.png", "image/png", "<svg onload='alert(1)'/>".getBytes())));
        assertThrows(AuthFlowException.class, () -> images.upload("owner", new MockMultipartFile("image", "large.png", "image/png", new byte[5*1024*1024+1])));
        byte[] huge = png(4097, 1);
        assertThrows(AuthFlowException.class, () -> images.upload("owner", new MockMultipartFile("image", "wide.png", "image/png", huge)));
        assertEquals(1, db.queryForObject("SELECT count(*) FROM admin_content_images", Integer.class));
    }
    @Test void directoryIncludesRegistrationsWithoutProfilesAndDoesNotInventActivity() {
        var provider = mock(CognitoIdentityProviderClient.class);
        String activeSub = UUID.randomUUID().toString(), pendingSub = UUID.randomUUID().toString(); UUID profile = UUID.randomUUID();
        db.update("INSERT INTO users(id,cognito_sub,email,last_sign_in_at,last_seen_at,login_count) VALUES (?,?,'active@example.test',now(),now(),3)", profile, activeSub);
        db.update("INSERT INTO user_favorites(user_id,item_type,item_id) VALUES (?,'prayer','fixture')", profile);
        when(provider.listUsers(any(ListUsersRequest.class))).thenReturn(ListUsersResponse.builder().users(account(activeSub,"active@example.test",true,UserStatusType.CONFIRMED)).paginationToken("next").build(), ListUsersResponse.builder().users(account(pendingSub,"pending@example.test",false,UserStatusType.UNCONFIRMED)).build());
        var directory = new AdminDirectory(db, provider, new AuthProperties(true,"","public-pool","public-client"));
        var result = transaction.execute(status -> directory.list("",0,"all","all",false));
        @SuppressWarnings("unchecked") List<Map<String,Object>> items = (List<Map<String,Object>>) result.get("items");
        var pending = items.stream().filter(row -> row.get("id").equals(pendingSub)).findFirst().orElseThrow();
        assertNull(pending.get("profile_id")); assertNull(pending.get("last_seen_at")); assertEquals(0L,pending.get("login_count"));
        assertEquals(0, db.queryForObject("SELECT count(*) FROM users WHERE cognito_sub=?", Integer.class, pendingSub));
        assertEquals(1, ((List<?>)transaction.execute(status -> directory.list("pending",0,"unconfirmed","never",false)).get("items")).size());
        assertEquals(1, ((List<?>)transaction.execute(status -> directory.list("active@example",0,"confirmed","saved",false)).get("items")).size());
        verify(provider,times(2)).listUsers(any(ListUsersRequest.class));
        when(provider.listUsers(any(ListUsersRequest.class))).thenThrow(InternalErrorException.builder().message("Synthetic failure").build());
        assertThrows(InternalErrorException.class, () -> transaction.execute(status -> directory.list("",0,"all","all",true)));
        assertEquals(2,db.queryForObject("SELECT count(*) FROM admin_user_directory",Integer.class));
        db.update("DELETE FROM users WHERE id=?", profile);
        assertEquals(0,db.queryForObject("SELECT count(*) FROM admin_user_directory WHERE cognito_sub=?",Integer.class,activeSub));
    }
    @Test void serverSessionAllowsEightHourIdleWindowButAlwaysEnforcesAbsoluteExpiry() {
        var properties = new AdminProperties(true,"owner-sub","admin-client",Base64.getEncoder().encodeToString(new byte[32]));
        var crypto = new AdminCrypto(properties);
        var service = new AdminAuthService(db,mock(CognitoIdentityProviderClient.class),new AuthProperties(true,"","pool","client"),properties,crypto,json);
        String token=crypto.token();
        db.update("INSERT INTO admin_sessions(token_hash,owner_sub,last_seen_at,expires_at) VALUES (?,'owner-sub',now()-interval '7 hours',now()+interval '1 hour')",AdminCrypto.hash(token));
        assertEquals("owner-sub",service.authenticate("Bearer "+token));
        assertEquals(8, service.sessionInfo("Bearer "+token).get("idleHours"));
        db.update("UPDATE admin_sessions SET last_seen_at=now()-interval '9 hours'");
        assertThrows(AuthFlowException.class,()->service.authenticate("Bearer "+token));
        db.update("UPDATE admin_sessions SET last_seen_at=now(), expires_at=now()-interval '1 second'");
        assertThrows(AuthFlowException.class,()->service.authenticate("Bearer "+token));
    }
    private UserType account(String sub,String email,boolean verified,UserStatusType status) {
        return UserType.builder().username(sub).enabled(true).userStatus(status).userCreateDate(Instant.parse("2026-01-01T00:00:00Z")).attributes(AttributeType.builder().name("sub").value(sub).build(),AttributeType.builder().name("email").value(email).build(),AttributeType.builder().name("email_verified").value(Boolean.toString(verified)).build()).build();
    }
    private byte[] png(int width,int height) throws IOException {
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);return output.toByteArray();
    }
}
