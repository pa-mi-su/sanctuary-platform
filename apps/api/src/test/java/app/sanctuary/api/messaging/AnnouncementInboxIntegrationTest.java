package app.sanctuary.api.messaging;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL",matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class AnnouncementInboxIntegrationTest {
 static JdbcTemplate db;static TransactionTemplate tx;
 PushMessages messages;PushGateway gateway;AnnouncementInboxController inbox;
 @BeforeAll static void setup(){var source=new DriverManagerDataSource(System.getenv("ADMIN_TEST_DB_URL"),"sanctuary_test","sanctuary_test");Flyway.configure().dataSource(source).load().migrate();db=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));}
 @BeforeEach void reset(){db.execute("TRUNCATE announcement_receipts,announcement_inbox_preferences,announcements,push_deliveries,push_messages,push_devices,push_registration_limits");gateway=mock(PushGateway.class);messages=new PushMessages(db,gateway,"https://dev.mydailysanctuary.com");inbox=new AnnouncementInboxController(db);}
 UUID publish(){var d=new PushMessages.Draft("An invitation","Full announcement text","home",null,"all",null,true,false);var p=(Map<?,?>)messages.preview(d);UUID id=UUID.randomUUID();tx.execute(s->messages.send("owner",new PushMessages.Send(id,d,p.get("recipientVersion").toString())));return id;}
 Jwt member(){String sub=UUID.randomUUID().toString();db.update("INSERT INTO users(cognito_sub,last_sign_in_at)VALUES(?,now())",sub);return Jwt.withTokenValue("synthetic").header("alg","none").subject(sub).build();}
 List<?> feed(){return (List<?>)((Map<?,?>)inbox.list(0)).get("items");}
 @Test void publishesWithoutPushPermissionDevicesOrEnabledFirebase(){
  UUID id=publish();assertEquals(1,feed().size());assertEquals(0,db.queryForObject("SELECT count(*) FROM push_deliveries",Integer.class));
  var entry=(Map<?,?>)inbox.detail(id);assertEquals("Full announcement text",entry.get("body"));assertEquals("https://dev.mydailysanctuary.com/",entry.get("destination"));verify(gateway,never()).send(anyString(),anyString(),anyString(),anyString(),anyString());
 }
 @Test void legacyAlertsAndTestsNeverBecomePublicAnnouncements(){
  UUID user=UUID.randomUUID(),device=UUID.randomUUID();String sub=UUID.randomUUID().toString();
  db.update("INSERT INTO users(id,cognito_sub,email,last_sign_in_at)VALUES(?,?,'pmsulikowski@icloud.com',now())",user,sub);
  db.update("INSERT INTO admin_user_directory(cognito_sub,email,email_verified,enabled,status,sync_id,registered_at)VALUES(?,'pmsulikowski@icloud.com',true,true,'CONFIRMED',?,now())",sub,UUID.randomUUID());
  db.update("INSERT INTO push_devices(id,user_id,installation_id,token,platform,app_version,enabled)VALUES(?,?,?,'synthetic-inbox-token','ios','test',true)",device,user,UUID.randomUUID());when(gateway.enabled()).thenReturn(true);
  for(var d:List.of(new PushMessages.Draft("Legacy","Message","home",null,"all",null),new PushMessages.Draft("Test","Private test","home",null,"test",device,true,true))){var p=(Map<?,?>)messages.preview(d);tx.execute(s->messages.send("owner",new PushMessages.Send(UUID.randomUUID(),d,p.get("recipientVersion").toString())));}
  assertTrue(feed().isEmpty());assertEquals(2,db.queryForObject("SELECT count(*) FROM push_deliveries",Integer.class));
 }
 @Test void publishedAlertsOpenTheAnnouncementWhileItsContentLinkStaysStable(){
  UUID user=UUID.randomUUID();db.update("INSERT INTO users(id,cognito_sub,last_sign_in_at)VALUES(?,?,now())",user,UUID.randomUUID().toString());
  db.update("INSERT INTO push_devices(user_id,installation_id,token,platform,app_version,enabled)VALUES(?,?,'synthetic-public-token','android','test',true)",user,UUID.randomUUID());when(gateway.enabled()).thenReturn(true);
  var d=new PushMessages.Draft("Published","Message","home",null,"all",null,true,true);var p=(Map<?,?>)messages.preview(d);UUID id=UUID.randomUUID();tx.execute(s->messages.send("owner",new PushMessages.Send(id,d,p.get("recipientVersion").toString())));
  assertEquals("https://dev.mydailysanctuary.com/announcements/"+id,db.queryForObject("SELECT destination FROM push_messages WHERE id=?",String.class,id));
  assertEquals("https://dev.mydailysanctuary.com/",((Map<?,?>)inbox.detail(id)).get("destination"));
 }
 @Test void readClearAndUndoBelongOnlyToTheAuthenticatedAccount(){
  UUID id=publish();var a=member();var b=member();
  tx.execute(s->inbox.change(a,new AnnouncementInboxController.Change("read",List.of(id),null)));
  tx.execute(s->inbox.change(a,new AnnouncementInboxController.Change("clear",List.of(id),null)));
  assertEquals(List.of(id),((Map<?,?>)inbox.state(a)).get("readIds"));assertEquals(List.of(id),((Map<?,?>)inbox.state(a)).get("clearedIds"));
  assertEquals(List.of(),((Map<?,?>)inbox.state(b)).get("readIds"));assertEquals(List.of(),((Map<?,?>)inbox.state(b)).get("clearedIds"));
  tx.execute(s->inbox.change(a,new AnnouncementInboxController.Change("restore",List.of(id),null)));
  assertEquals(List.of(),((Map<?,?>)inbox.state(a)).get("clearedIds"));assertEquals(1,feed().size());
  assertThrows(Exception.class,()->inbox.state(null));
 }
 @Test void clearAllIsAccountScopedAndDoesNotAffectNewerAnnouncements(){
  UUID id=publish();var a=member();var b=member();tx.execute(s->inbox.change(a,new AnnouncementInboxController.Change("clearAll",List.of(),id)));
  double cutoff=((Number)((Map<?,?>)inbox.state(a)).get("clearedBefore")).doubleValue();assertTrue(cutoff>0);assertEquals(0,((Number)((Map<?,?>)inbox.state(b)).get("clearedBefore")).intValue());
  UUID newer=publish();db.update("UPDATE announcements SET published_at=now()+interval '1 second' WHERE id=?",newer);assertTrue(((Number)((Map<?,?>)inbox.detail(newer)).get("published_at")).doubleValue()>cutoff);
 }
 @Test void clearingAdminHistoryDoesNotWithdrawAnAnnouncement(){
  UUID id=publish();tx.execute(s->messages.clearHistory(new PushMessages.ClearHistory(id)));assertEquals(1,feed().size());assertEquals(1,((List<?>)((Map<?,?>)messages.published(0)).get("items")).size());
  messages.withdraw(id);assertTrue(feed().isEmpty());assertThrows(Exception.class,()->inbox.detail(id));
  assertEquals(1,db.queryForObject("SELECT count(*) FROM push_messages",Integer.class));
 }
 @Test void olderInstalledAppsReceiveTheCompatibleContentLinkDuringRollout(){
  UUID user=UUID.randomUUID();db.update("INSERT INTO users(id,cognito_sub,last_sign_in_at)VALUES(?,?,now())",user,UUID.randomUUID().toString());
  db.update("INSERT INTO push_devices(user_id,installation_id,token,platform,app_version,enabled)VALUES(?,?,'synthetic-legacy-inbox-token','ios','1.0.24',true)",user,UUID.randomUUID());
  db.update("INSERT INTO push_devices(user_id,installation_id,token,platform,app_version,enabled)VALUES(?,?,'synthetic-current-inbox-token','android','1.0.26-dev',true)",user,UUID.randomUUID());
  when(gateway.enabled()).thenReturn(true);when(gateway.send(anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn(new PushGateway.Result(true,false,null,"provider"));
  var draft=new PushMessages.Draft("Public","Message","home",null,"all",null,true,true);var preview=(Map<?,?>)messages.preview(draft);UUID id=UUID.randomUUID();tx.execute(s->messages.send("owner",new PushMessages.Send(id,draft,preview.get("recipientVersion").toString())));
  new PushDispatcher(db,gateway).dispatch();
  verify(gateway).send(eq("synthetic-legacy-inbox-token"),anyString(),anyString(),eq("https://dev.mydailysanctuary.com/"),eq(id.toString()));
  verify(gateway).send(eq("synthetic-current-inbox-token"),anyString(),anyString(),eq("https://dev.mydailysanctuary.com/announcements/"+id),eq(id.toString()));
 }

}
