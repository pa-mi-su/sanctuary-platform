package app.sanctuary.api.news;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.support.TransactionTemplate;
import app.sanctuary.api.config.AuthProperties;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="NEWS_TEST_DB",matches="jdbc:postgresql://127\\.0\\.0\\.1:15439/news_cms_test")
class NewsAuthStateStoreIntegrationTest {
    static JdbcTemplate db;
    static TransactionTemplate tx;
    static final String KEY="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
    static final AuthProperties AUTH=new AuthProperties(true,"client","pool","client");
    NewsAuthStateStore first,second;

    @BeforeAll static void database() {
        var base=new DriverManagerDataSource(System.getenv("NEWS_TEST_DB"),"postgres","local-cms-test");
        new JdbcTemplate(base).execute("DROP SCHEMA IF EXISTS cms_auth_security CASCADE");
        new JdbcTemplate(base).execute("CREATE SCHEMA cms_auth_security");
        var ds=new DriverManagerDataSource(System.getenv("NEWS_TEST_DB")+"?currentSchema=cms_auth_security","postgres","local-cms-test");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V16__create_news_cms.sql"),new ClassPathResource("db/migration/V17__create_news_mfa_sessions.sql"),new ClassPathResource("db/migration/V19__harden_news_auth.sql")).execute(ds);
        db=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
    }
    @BeforeEach void setup() {
        db.execute("TRUNCATE news_auth_challenges,news_auth_attempts");
        first=new NewsAuthStateStore(db,KEY,new MockEnvironment(),AUTH);
        second=new NewsAuthStateStore(db,KEY,new MockEnvironment(),AUTH);
    }
    String put(Instant expires) {
        String id=UUID.randomUUID().toString();
        tx.executeWithoutResult(status->first.put(id,new NewsAuthStateStore.Pending("admin","cognito-session","private-access-token","private-id-token",expires,0)));
        return id;
    }
    @Test void replicasShareEncryptedStateAndConsumeExactlyOnce()throws Exception {
        String id=put(Instant.now().plusSeconds(300));
        byte[] ciphertext=db.queryForObject("SELECT payload FROM news_auth_challenges WHERE id=?",byte[].class,UUID.fromString(id));
        assertThat(new String(ciphertext,java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("private-access-token","private-id-token","cognito-session");
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->first.take(id));var b=pool.submit(()->second.take(id));
            assertThat(java.util.stream.Stream.of(a.get(),b.get()).filter(Objects::nonNull).count()).isEqualTo(1);
        }
        assertThat(first.take(id)).isNull();
    }
    @Test void expiredTamperedAndWrongKeyChallengesFailClosed() {
        assertThat(second.take(put(Instant.now().minusSeconds(1)))).isNull();
        String id=put(Instant.now().plusSeconds(300));
        db.update("UPDATE news_auth_challenges SET payload=decode('000000','hex') WHERE id=?",UUID.fromString(id));
        assertThat(second.take(id)).isNull();
        id=put(Instant.now().plusSeconds(300));
        var wrongKey=new NewsAuthStateStore(db,"AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=",new MockEnvironment(),AUTH);
        assertThat(wrongKey.take(id)).isNull();assertThat(first.take(id)).isNull();
    }
    @Test void accountRateLimitIsSharedNormalizedAndExpires() {
        for(int i=0;i<20;i++)assertThat(tx.<Boolean>execute(status->first.allowLogin("ADMIN@example.org"))).isTrue();
        assertThat(tx.<Boolean>execute(status->second.allowLogin("admin@example.org"))).isFalse();
        assertThat(db.queryForObject("SELECT key_hash FROM news_auth_attempts",String.class)).doesNotContain("admin");
        db.update("UPDATE news_auth_attempts SET expires_at=now()-interval '1 second'");
        assertThat(tx.<Boolean>execute(status->second.allowLogin("admin@example.org"))).isTrue();
    }
    @Test void deployedAuthenticationRequiresEncryptionKey() {
        assertThatThrownBy(()->new NewsAuthStateStore(db,"",new MockEnvironment().withProperty("spring.profiles.active","prod"),AUTH))
            .isInstanceOf(IllegalStateException.class);
    }
}
