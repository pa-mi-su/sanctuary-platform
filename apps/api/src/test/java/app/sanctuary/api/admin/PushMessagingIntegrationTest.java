package app.sanctuary.api.admin;
import app.sanctuary.api.messaging.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL",matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class PushMessagingIntegrationTest {
    static JdbcTemplate db; static TransactionTemplate transaction;
    PushGateway gateway; PushMessages messages;
    @BeforeAll static void setup(){var source=new DriverManagerDataSource(System.getenv("ADMIN_TEST_DB_URL"),"sanctuary_test","sanctuary_test");Flyway.configure().dataSource(source).load().migrate();db=new JdbcTemplate(source);transaction=new TransactionTemplate(new DataSourceTransactionManager(source));}
    @BeforeEach void reset(){db.execute("TRUNCATE push_deliveries,push_messages,push_devices");gateway=mock(PushGateway.class);when(gateway.enabled()).thenReturn(true);messages=new PushMessages(db,gateway);}
    UUID device(String platform,boolean enabled){UUID user=UUID.randomUUID(),device=UUID.randomUUID();db.update("INSERT INTO users(id,cognito_sub,email,last_sign_in_at)VALUES(?,?,?,now())",user,"push-test-"+user,"push@example.test");db.update("INSERT INTO push_devices(id,user_id,installation_id,token,platform,app_version,enabled)VALUES(?,?,?,?,?,'1.0.16',?)",device,user,UUID.randomUUID(),"synthetic-token-"+device,platform,enabled);return device;}
    PushMessages.Draft draft(String audience){return new PushMessages.Draft("A moment of peace","An invitation to prayer","home",null,audience,null);}
    @SuppressWarnings("unchecked") Map<String,Object> preview(PushMessages.Draft draft){return (Map<String,Object>)messages.preview(draft);}
    @Test void optInFilteringAudienceReviewIdempotencyAndDispatcher(){
        device("ios",true);device("android",false);var draft=draft("all");var preview=preview(draft);assertEquals(1,preview.get("count"));
        var request=new PushMessages.Send(UUID.randomUUID(),draft,preview.get("recipientVersion").toString());
        transaction.execute(s->messages.send("owner",request));transaction.execute(s->messages.send("owner",request));
        assertEquals(1,db.queryForObject("SELECT count(*)FROM push_messages",Integer.class));
        when(gateway.send(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new PushGateway.Result(true,false,null,"provider-id"));
        var dispatcher=new PushDispatcher(db,gateway);dispatcher.dispatch();dispatcher.dispatch();
        verify(gateway,times(1)).send(anyString(),eq(draft.title()),eq(draft.body()),eq("https://mydailysanctuary.com/"),anyString());
        assertEquals("accepted",db.queryForObject("SELECT state FROM push_deliveries",String.class));
        assertFalse(new ObjectMapper().valueToTree(messages.overview(0)).toString().contains("synthetic-token"));
    }
    @Test void changedAudienceRejectsSendAndOptOutAfterQueueSkips(){
        UUID device=device("ios",true);var draft=draft("ios");var preview=preview(draft);
        db.update("UPDATE push_devices SET enabled=false WHERE id=?",device);
        assertThrows(RuntimeException.class,()->transaction.execute(s->messages.send("owner",new PushMessages.Send(UUID.randomUUID(),draft,preview.get("recipientVersion").toString()))));
        db.update("UPDATE push_devices SET enabled=true WHERE id=?",device);
        transaction.execute(s->messages.send("owner",new PushMessages.Send(UUID.randomUUID(),draft,preview(draft).get("recipientVersion").toString())));
        db.update("UPDATE push_devices SET enabled=false WHERE id=?",device);new PushDispatcher(db,gateway).dispatch();
        verify(gateway,never()).send(anyString(),anyString(),anyString(),anyString(),anyString());assertEquals("skipped",db.queryForObject("SELECT state FROM push_deliveries",String.class));
    }
    @Test void deviceAccountTransferCannotReceivePreviouslyQueuedNotifications() {
        UUID id=device("ios",true);var draft=draft("all");
        transaction.execute(s->messages.send("owner",new PushMessages.Send(UUID.randomUUID(),draft,preview(draft).get("recipientVersion").toString())));
        var row=db.queryForMap("SELECT * FROM push_devices WHERE id=?",id);
        UUID newUser=UUID.randomUUID();db.update("INSERT INTO users(id,cognito_sub,last_sign_in_at)VALUES(?,?,now())",newUser,"push-transfer-"+newUser);
        var accounts=mock(app.sanctuary.api.user.service.UserAccountService.class);
        var account=mock(app.sanctuary.api.user.dto.UserAccountDto.class);when(account.id()).thenReturn(newUser);when(accounts.ensureAccount(any())).thenReturn(account);
        var jwt=org.springframework.security.oauth2.jwt.Jwt.withTokenValue("synthetic").header("alg","none").subject("new-user").build();
        var authentication=new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt);
        var controller=new PushDevicesController(db,accounts);
        transaction.execute(s->controller.register(authentication,new PushDevicesController.Device((UUID)row.get("installation_id"),row.get("token").toString(),"ios","1.0.16",true)));
        new PushDispatcher(db,gateway).dispatch();verify(gateway,never()).send(anyString(),anyString(),anyString(),anyString(),anyString());
        assertEquals("skipped",db.queryForObject("SELECT state FROM push_deliveries",String.class));
        assertEquals(newUser,db.queryForObject("SELECT user_id FROM push_devices WHERE id=?",UUID.class,id));
    }
    @Test void testAudienceExcludesUnverifiedOrNonOwnerAccounts() {
        UUID id=device("ios",true);
        var test=new PushMessages.Draft("Test","Message","home",null,"test",id);
        assertEquals(0,preview(test).get("count"));
        String subject=db.queryForObject("SELECT u.cognito_sub FROM users u JOIN push_devices d ON d.user_id=u.id WHERE d.id=?",String.class,id);
        db.update("INSERT INTO admin_user_directory(cognito_sub,email,email_verified,status,enabled,registered_at,sync_id)VALUES(?,'pmsulikowski@icloud.com',false,'CONFIRMED',true,now(),?)",subject,UUID.randomUUID());
        assertEquals(0,preview(test).get("count"));db.update("UPDATE admin_user_directory SET email_verified=true WHERE cognito_sub=?",subject);
        assertEquals(1,preview(test).get("count"));
        db.update("UPDATE admin_user_directory SET email='someone@example.test' WHERE cognito_sub=?",subject);
        assertEquals(0,preview(test).get("count"));
    }
    @Test void rejectsUnknownContentAndDisabledGatewayAndDoesNotResendAmbiguousClaims(){
        assertThrows(RuntimeException.class,()->preview(new PushMessages.Draft("Title","Message","saints","missing","all",null)));
        device("ios",true);when(gateway.enabled()).thenReturn(false);var draft=draft("all");
        assertThrows(RuntimeException.class,()->transaction.execute(s->messages.send("owner",new PushMessages.Send(UUID.randomUUID(),draft,preview(draft).get("recipientVersion").toString()))));
        when(gateway.enabled()).thenReturn(true);transaction.execute(s->messages.send("owner",new PushMessages.Send(UUID.randomUUID(),draft,preview(draft).get("recipientVersion").toString())));
        db.update("UPDATE push_deliveries SET state='sending',updated_at=now()-interval '11 minutes'");new PushDispatcher(db,gateway).dispatch();
        verify(gateway,never()).send(anyString(),anyString(),anyString(),anyString(),anyString());assertEquals("unknown",db.queryForObject("SELECT state FROM push_deliveries",String.class));
    }
}
