package app.sanctuary.api.messaging;

import java.util.*;
import app.sanctuary.api.auth.service.AuthFlowException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL", matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class AnnouncementGuestTransferTest {
    static JdbcTemplate db;
    AnnouncementInboxController controller;
    UUID first, second, announcement;
    Jwt jwt(String subject) { return Jwt.withTokenValue("local-test").header("alg","none").subject(subject).build(); }
    @BeforeAll static void database() {
        var source = new DriverManagerDataSource(System.getenv("ADMIN_TEST_DB_URL"),"sanctuary_test","sanctuary_test");
        Flyway.configure().dataSource(source).load().migrate(); db = new JdbcTemplate(source);
    }
    @BeforeEach void setup() {
        controller = new AnnouncementInboxController(db);
        first=UUID.randomUUID(); second=UUID.randomUUID(); announcement=UUID.randomUUID();
        db.update("INSERT INTO users(id,cognito_sub,last_sign_in_at) VALUES(?,?,now()),(?,?,now())",first,first.toString(),second,second.toString());
        db.update("INSERT INTO announcements(id,title,body,destination) VALUES(?,'Test','Test','https://mydailysanctuary.com/')",announcement);
    }
    @AfterEach void cleanup() {
        db.update("DELETE FROM users WHERE id IN (?,?)",first,second);
        db.update("DELETE FROM announcements WHERE id=?",announcement);
    }
    @Test void transfersReadAndClearedStateIdempotentlyWithoutAffectingAnotherAccount() {
        var guest=new AnnouncementInboxController.GuestState(List.of(announcement),List.of(announcement),0);
        controller.mergeGuest(jwt(first.toString()),guest); controller.mergeGuest(jwt(first.toString()),guest);
        var state=(Map<?,?>)controller.state(jwt(first.toString()));
        assertEquals(List.of(announcement),state.get("readIds")); assertEquals(List.of(announcement),state.get("clearedIds"));
        var other=(Map<?,?>)controller.state(jwt(second.toString())); assertEquals(List.of(),other.get("clearedIds"));
        assertEquals(1,db.queryForObject("SELECT count(*) FROM announcement_receipts WHERE user_id=?",Integer.class,first));
    }
    @Test void mergesClearAllAndIgnoresDeletedAnnouncementIds() {
        double before=System.currentTimeMillis()-1000;
        db.update("UPDATE users SET created_at=now()-interval '1 day' WHERE id=?",first);
        controller.mergeGuest(jwt(first.toString()),new AnnouncementInboxController.GuestState(List.of(UUID.randomUUID()),List.of(),before));
        var state=(Map<?,?>)controller.state(jwt(first.toString()));
        assertEquals(before,((Number)state.get("clearedBefore")).doubleValue(),1);
        assertEquals(List.of(),state.get("readIds"));
    }
    @Test void rejectsAnonymousAndInvalidTransfers() {
        var valid=new AnnouncementInboxController.GuestState(List.of(),List.of(),0);
        assertThrows(AuthFlowException.class,()->controller.mergeGuest(null,valid));
        assertThrows(AuthFlowException.class,()->controller.mergeGuest(jwt(first.toString()),new AnnouncementInboxController.GuestState(List.of(),List.of(),Double.NaN)));
        assertThrows(AuthFlowException.class,()->controller.mergeGuest(jwt(first.toString()),new AnnouncementInboxController.GuestState(List.of(),List.of(),System.currentTimeMillis()+100000)));
    }
}
