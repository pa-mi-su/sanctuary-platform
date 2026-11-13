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
    @BeforeEach void reset(){db.execute("TRUNCATE push_deliveries,push_messages,push_devices,push_registration_limits");gateway=mock(PushGateway.class);when(gateway.enabled()).thenReturn(true);messages=new PushMessages(db,gateway);}
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
    @Test void devDestinationsStayOnDevAndProductionStaysOnProduction() {
        var dev=new PushMessages(db,gateway,"https://dev.mydailysanctuary.com");
        assertEquals("https://dev.mydailysanctuary.com/",((Map<?,?>)dev.preview(draft("all"))).get("destination"));
        String prayerId="push-link-"+UUID.randomUUID();
        db.update("INSERT INTO prayers(id,slug,category,title_en,title_es,title_pl,body_en,body_es,body_pl,alternate_title_en,alternate_title_es,alternate_title_pl,note_en,note_es,note_pl,source_title,source_type)VALUES(?,?,'test','','','','','','','','','','','','','','')",prayerId,prayerId);
        var prayer=Map.of("id",prayerId,"slug",prayerId);
        var draft=new PushMessages.Draft("Prayer","Message","prayers",prayer.get("id").toString(),"all",null);
        assertEquals("https://dev.mydailysanctuary.com/prayers/"+prayer.get("slug"),((Map<?,?>)dev.preview(draft)).get("destination"));
        assertEquals("https://mydailysanctuary.com/prayers/"+prayer.get("slug"),preview(draft).get("destination"));
        assertThrows(IllegalArgumentException.class,()->new PushMessages(db,gateway,"https://untrusted.example"));
        db.update("DELETE FROM prayers WHERE id=?",prayerId);
    }
    @Test void retiringOneRegistrationExcludesItAndPreservesItsSendHistory() {
        UUID id=device("android",true);var draft=draft("all");var message=UUID.randomUUID();
        transaction.execute(t->messages.send("owner",new PushMessages.Send(message,draft,preview(draft).get("recipientVersion").toString())));
        messages.retireDevice(id);
        assertEquals(0,preview(draft).get("count"));
        var json=new ObjectMapper().valueToTree(messages.deliveryDetails(message));
        assertEquals(id.toString(),json.at("/devices/0/device_id").asText());
        assertEquals("pending",json.at("/devices/0/state").asText());
        assertFalse(json.toString().contains("synthetic-token"));
        assertFalse(json.toString().contains("installation_secret"));
    }
    @Test void previewIdentifiesRecipientsWithoutExposingDeliveryCredentials() {
        UUID id=device("ios",true);device("android",false);
        var result=preview(draft("all"));
        var json=new ObjectMapper().valueToTree(result);
        assertEquals(1,json.get("devices").size());
        assertEquals(id.toString(),json.at("/devices/0/id").asText());
        assertEquals("push@example.test",json.at("/devices/0/email").asText());
        assertTrue(json.at("/devices/0/signedIn").asBoolean());
        assertFalse(json.toString().contains("synthetic-token"));
        assertFalse(json.toString().contains("installation_secret"));
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
    @Test void anonymousInstallationsReceiveGuestBroadcastsWithoutCreatingAccounts() {
        var registry=new PushInstallationRegistry(db);int users=db.queryForObject("SELECT count(*) FROM users",Integer.class);
        var device=new PushDevicesController.Device(UUID.randomUUID(),"guest-token-"+UUID.randomUUID(),"ios","1.0.17",true,"A".repeat(64));
        transaction.execute(s->registry.register(device,null,true));
        assertEquals(users,db.queryForObject("SELECT count(*) FROM users",Integer.class));
        assertEquals(1,preview(draft("guests")).get("count"));assertEquals(0,preview(draft("members")).get("count"));assertEquals(1,preview(draft("all")).get("count"));
        var guestDraft=draft("guests");transaction.execute(s->messages.send("owner",new PushMessages.Send(UUID.randomUUID(),guestDraft,preview(guestDraft).get("recipientVersion").toString())));
        when(gateway.send(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new PushGateway.Result(true,false,null,"guest-send"));
        new PushDispatcher(db,gateway).dispatch();assertEquals("accepted",db.queryForObject("SELECT state FROM push_deliveries",String.class));
        transaction.execute(s->registry.register(new PushDevicesController.Device(device.installationId(),device.token(),"ios","1.0.17",false,device.installationSecret()),null,true));
        assertEquals(0,preview(draft("all")).get("count"));
    }
    @Test void guessedInstallationIdsAndForeignCredentialsCannotTakeOverDevice() {
        var registry=new PushInstallationRegistry(db);
        var device=new PushDevicesController.Device(UUID.randomUUID(),"protected-token-"+UUID.randomUUID(),"android","1.0.17",true,"A".repeat(64));
        transaction.execute(s->registry.register(device,null,true));
        assertThrows(RuntimeException.class,()->transaction.execute(s->registry.register(new PushDevicesController.Device(device.installationId(),device.token(),"android","1.0.17",false,"B".repeat(64)),null,true)));
        assertThrows(RuntimeException.class,()->transaction.execute(s->registry.register(new PushDevicesController.Device(UUID.randomUUID(),device.token(),"android","1.0.17",true,"B".repeat(64)),null,true)));
        assertThrows(RuntimeException.class,()->transaction.execute(s->registry.register(new PushDevicesController.Device(device.installationId(),device.token(),"android","1.0.17",true),null,true)));
        assertEquals(1,preview(draft("guests")).get("count"));
    }
    @Test void accountDetachesOnAnonymousRegistrationWhileConsentSurvives() {
        UUID id=device("ios",true);var row=db.queryForMap("SELECT * FROM push_devices WHERE id=?",id);
        UUID owner=(UUID)row.get("user_id");var registry=new PushInstallationRegistry(db);
        var request=new PushDevicesController.Device((UUID)row.get("installation_id"),row.get("token").toString(),"ios","1.0.17",true,"C".repeat(64));
        transaction.execute(s->registry.register(request,owner,false));assertEquals(1,preview(draft("members")).get("count"));
        transaction.execute(s->registry.register(request,null,true));assertEquals(1,preview(draft("guests")).get("count"));assertEquals(0,preview(draft("members")).get("count"));
        assertNull(db.queryForObject("SELECT user_id FROM push_devices WHERE id=?",UUID.class,id));
    }
    @Test void aLateSignedOutRequestCannotUndoNewerSignedInRegistration() {
        UUID id=device("ios",true);var row=db.queryForMap("SELECT * FROM push_devices WHERE id=?",id);
        var registry=new PushInstallationRegistry(db);UUID owner=(UUID)row.get("user_id");
        var newest=new PushDevicesController.Device((UUID)row.get("installation_id"),row.get("token").toString(),"ios","1.0.17",true,"D".repeat(64),2L);
        transaction.execute(s->registry.register(newest,owner,false));
        var old=new PushDevicesController.Device(newest.installationId(),newest.token(),"ios","1.0.17",true,newest.installationSecret(),1L);
        transaction.execute(s->registry.register(old,null,true));
        assertEquals(owner,db.queryForObject("SELECT user_id FROM push_devices WHERE id=?",UUID.class,id));
        assertEquals(1,preview(draft("members")).get("count"));
    }
    @Test void publicRegistrationRateLimitIsBounded() {
        long window=java.time.Instant.now().getEpochSecond()/3600;
        db.update("INSERT INTO push_registration_limits(bucket,window_start,requests)VALUES('global',?,10000)",window);
        assertThrows(RuntimeException.class,()->transaction.execute(s->{new PushRegistrationLimiter(db).check("127.0.0.2");return null;}));
        assertEquals(10000,db.queryForObject("SELECT requests FROM push_registration_limits WHERE bucket='global'",Integer.class));
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
