package app.sanctuary.api.user.service;

import app.sanctuary.api.user.dto.ContentReminder;
import app.sanctuary.api.user.repository.UserAccountRepository;
import app.sanctuary.api.user.web.ContentReminderController;
import app.sanctuary.api.auth.service.CognitoAuthService;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.interceptor.*;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL", matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class ContentReminderIntegrationTest {
    static JdbcTemplate db;
    static ContentReminderService service;
    MockMvc http;
    @BeforeAll static void database() {
        var source = new DriverManagerDataSource(System.getenv("ADMIN_TEST_DB_URL"), "sanctuary_test", "sanctuary_test");
        Flyway.configure().dataSource(source).load().migrate();
        db = new JdbcTemplate(source);
        var factory = new ProxyFactory(new ContentReminderService(db));
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(source), new AnnotationTransactionAttributeSource()));
        service = (ContentReminderService) factory.getProxy();
    }
    @BeforeEach void setup() {
        db.update("DELETE FROM users WHERE cognito_sub LIKE 'reminder-test-%'");
        db.update("INSERT INTO prayers(id,slug,category,title_en,title_es,title_pl,body_en,body_es,body_pl,alternate_title_en,alternate_title_es,alternate_title_pl,note_en,note_es,note_pl,source_title,source_type) VALUES('reminder-test-prayer','reminder-test-prayer','prayer','Test prayer','Test prayer','Test prayer','Prayer','Prayer','Prayer','','','','','','','','') ON CONFLICT DO NOTHING");
        db.update("INSERT INTO novenas(id,slug,title_en,title_es,title_pl,description_en,description_es,description_pl,duration_days) VALUES('reminder-test-novena','reminder-test-novena','Test novena','Test novena','Test novena','','','',9) ON CONFLICT DO NOTHING");
        http = MockMvcBuilders.standaloneSetup(new ContentReminderController(service, new UserAccountService(new UserAccountRepository(db), mock(CognitoAuthService.class)))).build();
    }
    JwtAuthenticationToken account(String name) {
        return new JwtAuthenticationToken(Jwt.withTokenValue("local-test").header("alg","none").subject("reminder-test-"+name).claim("email",name+"@sanctuary.invalid").build());
    }
    String body(String kind, int hour, int minute) {
        return "{\"kind\":\""+kind+"\",\"slug\":\"reminder-test-"+(kind.equals("prayers")?"prayer":"novena")+"\",\"title\":\"Reminder\",\"hour\":"+hour+",\"minute\":"+minute+"}";
    }
    @Test void twoClientsSeeExactMinutesAndPropagateEditsDeletesAndClearWithoutCrossingAccounts() throws Exception {
        // Android creates 14:55; iOS reads the same account, not an independently derived trigger time.
        http.perform(put("/me/reminders").principal(account("a")).contentType(MediaType.APPLICATION_JSON).content(body("novenas",14,55))).andExpect(status().isNoContent());
        http.perform(get("/me/reminders").principal(account("a"))).andExpect(jsonPath("$[0].minute").value(55));
        http.perform(get("/me/reminders").principal(account("b"))).andExpect(jsonPath("$.length()").value(0));
        // iOS creates a prayer at 15:00; Android can now read both.
        http.perform(put("/me/reminders").principal(account("a")).contentType(MediaType.APPLICATION_JSON).content(body("prayers",15,0))).andExpect(status().isNoContent());
        http.perform(get("/me/reminders").principal(account("a"))).andExpect(jsonPath("$.length()").value(2));
        http.perform(put("/me/reminders").principal(account("a")).contentType(MediaType.APPLICATION_JSON).content(body("novenas",14,57))).andExpect(status().isNoContent());
        http.perform(get("/me/reminders").principal(account("a"))).andExpect(jsonPath("$[0].minute").value(57));
        http.perform(delete("/me/reminders/novenas/reminder-test-novena").principal(account("b"))).andExpect(status().isNoContent());
        http.perform(get("/me/reminders").principal(account("a"))).andExpect(jsonPath("$.length()").value(2));
        http.perform(delete("/me/reminders/novenas/reminder-test-novena").principal(account("a"))).andExpect(status().isNoContent());
        http.perform(get("/me/reminders").principal(account("a"))).andExpect(jsonPath("$.length()").value(1));
        http.perform(delete("/me/reminders").principal(account("a"))).andExpect(status().isNoContent());
        http.perform(get("/me/reminders").principal(account("a"))).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void rejectsMissingOutOfRangeAndInvalidContentBeforePersisting() throws Exception {
        for (String body : List.of(body("prayers",24,0), body("prayers",8,60), body("prayers",-1,0), body("saints",8,0), body("prayers",8,0).replace("reminder-test-prayer","../other"), body("prayers",8,0).replace("\"hour\":8","\"hour\":null"))) {
            http.perform(put("/me/reminders").principal(account("a")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        http.perform(put("/me/reminders").principal(account("a")).contentType(MediaType.APPLICATION_JSON).content(body("prayers",8,0).replace("reminder-test-prayer","no-such-content"))).andExpect(status().isBadRequest());
        http.perform(get("/me/reminders").principal(account("a"))).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void concurrentSavesRespectFiftyLimitAndAccountDeletionCascades() throws Exception {
        UUID owner = UUID.randomUUID();
        db.update("INSERT INTO users(id,cognito_sub,last_sign_in_at) VALUES(?,?,now())",owner,"reminder-test-limit");
        for (int n=0; n<49; n++) db.update("INSERT INTO user_content_reminders(user_id,kind,slug,title,hour,minute) VALUES(?,'prayers',?,'Old',8,0)",owner,"old-"+n);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var results = pool.invokeAll(List.<Callable<Boolean>>of(
                () -> save(owner,"prayers","reminder-test-prayer"), () -> save(owner,"novenas","reminder-test-novena")));
            assertEquals(1, results.stream().filter(f -> { try { return f.get(); } catch(Exception e) {throw new RuntimeException(e);} }).count());
            assertEquals(50,service.list(owner).size());
            db.update("DELETE FROM users WHERE id=?",owner);
            assertTrue(service.list(owner).isEmpty());
        } finally { pool.shutdownNow(); }
    }
    boolean save(UUID owner,String kind,String slug) {
        try { service.save(owner,new ContentReminder(kind,slug,"New",14,55)); return true; }
        catch (IllegalArgumentException limit) { return false; }
    }
}
