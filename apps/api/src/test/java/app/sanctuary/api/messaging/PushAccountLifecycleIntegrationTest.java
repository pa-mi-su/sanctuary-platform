package app.sanctuary.api.messaging;

import java.util.*;
import app.sanctuary.api.user.repository.UserAccountRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL",matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class PushAccountLifecycleIntegrationTest {
 static JdbcTemplate db; static TransactionTemplate tx;
 List<UUID> owners=new ArrayList<>(), installations=new ArrayList<>(), messages=new ArrayList<>();
 @BeforeAll static void setup(){
  var source=new DriverManagerDataSource(System.getenv("ADMIN_TEST_DB_URL"),"sanctuary_test","sanctuary_test");
  Flyway.configure().dataSource(source).load().migrate(); db=new JdbcTemplate(source);
  tx=new TransactionTemplate(new DataSourceTransactionManager(source));
 }
 UUID owner(){var id=UUID.randomUUID();owners.add(id);db.update("INSERT INTO users(id,cognito_sub,last_sign_in_at) VALUES(?,?,now())",id,id.toString());return id;}
 PushDevicesController.Device device(String platform,boolean enabled){
  var id=UUID.randomUUID();installations.add(id);
  return new PushDevicesController.Device(id,"synthetic-lifecycle-token-"+id,platform,"test",enabled,"L".repeat(64),1L);
 }
 void register(PushDevicesController.Device device,UUID owner){tx.execute(s->new PushInstallationRegistry(db).register(device,owner,owner==null));}
 Map<String,Object> stored(UUID installation){return db.queryForMap("SELECT id,user_id,token,enabled,valid,installation_secret_hash,registration_version FROM push_devices WHERE installation_id=?",installation);}
 @AfterEach void cleanup(){for(var id:messages){db.update("DELETE FROM push_deliveries WHERE message_id=?",id);db.update("DELETE FROM push_messages WHERE id=?",id);}for(var id:installations)db.update("DELETE FROM push_devices WHERE installation_id=?",id);for(var id:owners)db.update("DELETE FROM users WHERE id=?",id);}
 @Test void deletionDetachesBothPlatformsWithoutChangingConsentOrRegistration(){
  UUID account=owner(), other=owner();
  var ios=device("ios",true);var android=device("android",false);var untouched=device("ios",true);
  register(ios,account);register(android,account);register(untouched,other);
  var beforeIos=stored(ios.installationId());var beforeAndroid=stored(android.installationId());
  UUID message=UUID.randomUUID();messages.add(message);
  db.update("INSERT INTO push_messages(id,actor_sub,title,body,destination,audience)VALUES(?,'test','Test','Test','https://mydailysanctuary.com/','members')",message);
  db.update("INSERT INTO push_deliveries(message_id,device_id,recipient_user_id,state)VALUES(?,?,?,'pending')",message,beforeIos.get("id"),account);
  db.update("INSERT INTO push_deliveries(message_id,device_id,recipient_user_id,state)VALUES(?,?,?,'accepted')",message,beforeAndroid.get("id"),account);
  tx.executeWithoutResult(s->new UserAccountRepository(db).deleteById(account));
  assertEquals("skipped",db.queryForObject("SELECT state FROM push_deliveries WHERE message_id=? AND device_id=?",String.class,message,beforeIos.get("id")));
  assertEquals("accepted",db.queryForObject("SELECT state FROM push_deliveries WHERE message_id=? AND device_id=?",String.class,message,beforeAndroid.get("id")));
  beforeIos.put("user_id",null);beforeAndroid.put("user_id",null);
  assertEquals(beforeIos,stored(ios.installationId()));assertEquals(beforeAndroid,stored(android.installationId()));
  assertEquals(other,stored(untouched.installationId()).get("user_id"));
  assertEquals(0,db.queryForObject("SELECT count(*) FROM users WHERE id=?",Integer.class,account));
  register(ios,null);register(android,null); // Deleting phone can sync anonymously with the same credentials.
  assertEquals(true,stored(ios.installationId()).get("enabled"));assertEquals(false,stored(android.installationId()).get("enabled"));
 }
 @Test void loginLogoutAndAccountSwitchPreserveOptInsAndOptOutsOnBothPlatforms(){
  UUID first=owner(),second=owner();
  for(String platform:List.of("ios","android")) for(boolean enabled:List.of(true,false)){
   var device=device(platform,enabled);
   for(UUID owner:Arrays.asList(null,first,null,first,second,null)){
    register(device,owner);
    assertEquals(owner,stored(device.installationId()).get("user_id"));
    assertEquals(enabled,stored(device.installationId()).get("enabled"));
   }
  }
 }
}
