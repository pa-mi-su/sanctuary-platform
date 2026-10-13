package app.sanctuary.api.news;

import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="NEWS_TEST_DB",matches="jdbc:postgresql://127\\.0\\.0\\.1:15439/news_cms_test")
class NewsRepositoryIntegrationTest {
    static NewsRepository repo; static JdbcTemplate db; static TransactionTemplate tx;
    @BeforeAll static void setup(){
        var base=new DriverManagerDataSource(System.getenv("NEWS_TEST_DB"),"postgres","local-cms-test");
        new JdbcTemplate(base).execute("DROP SCHEMA IF EXISTS cms_repository_audit CASCADE");
        new JdbcTemplate(base).execute("CREATE SCHEMA cms_repository_audit");
        var ds=new DriverManagerDataSource(System.getenv("NEWS_TEST_DB")+"?currentSchema=cms_repository_audit","postgres","local-cms-test");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V16__create_news_cms.sql"),new ClassPathResource("db/migration/V17__create_news_mfa_sessions.sql"),new ClassPathResource("db/migration/V18__news_placement_and_trash.sql"),new ClassPathResource("db/migration/V19__harden_news_auth.sql")).execute(ds);
        db=new JdbcTemplate(ds);repo=new NewsRepository(db);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
    }
    @BeforeEach void clean(){db.execute("TRUNCATE news_audit,news_corrections,news_articles,news_images CASCADE");}
    NewsArticle article(String status,OffsetDateTime date,boolean lead,int version,UUID image){return new NewsArticle(null,"A verified story","Vatican","Summary","Article body","Reporter",image,"A church","Caption","Own photo","https://example.org",status,date,lead,version,null,List.of());}
    NewsArticle save(UUID id,NewsArticle a,boolean create){return tx.execute(s->repo.save(id,a,"owner-sub",create));}
    @Test void draftsAndTheirImagesAreNeverPublic(){
        UUID image=repo.image("image/png",new byte[]{1,2,3});UUID id=UUID.randomUUID();
        save(id,article("DRAFT",null,true,0,image),true);
        assertThat(repo.list(true)).hasSize(1);assertThat(repo.list(false)).isEmpty();
        assertThatThrownBy(()->repo.find(id,false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->repo.image(image,false)).isInstanceOf(ResponseStatusException.class);
        assertThat(repo.image(image,true).bytes()).hasSize(3);
        save(id,article("PUBLISHED",null,true,0,image),false);
        assertThat(repo.list(false)).hasSize(1);assertThat(repo.image(image,false).bytes()).hasSize(3);
        save(id,article("DRAFT",null,true,1,image),false);
        assertThat(repo.list(false)).isEmpty();assertThatThrownBy(()->repo.image(image,false)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void scheduledArticlesAppearOnlyWhenDue(){
        UUID id=UUID.randomUUID();save(id,article("SCHEDULED",OffsetDateTime.now().plusDays(1),false,0,null),true);
        assertThat(repo.list(false)).isEmpty();
        db.update("UPDATE news_articles SET publish_at=now()-interval '1 minute' WHERE id=?",id);
        assertThat(repo.list(false)).hasSize(1);
    }
    @Test void staleUpdatesConflictAndOnlyOneLeadExists(){
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        save(first,article("PUBLISHED",null,true,0,null),true);
        save(second,article("PUBLISHED",null,true,0,null),true);
        assertThat(repo.find(first,true).isLead()).isFalse();assertThat(repo.find(first,true).version()).isEqualTo(1);
        assertThat(repo.find(second,true).isLead()).isTrue();
        assertThatThrownBy(()->save(first,article("DRAFT",null,true,0,null),false)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        assertThat(repo.find(second,true).isLead()).isTrue();
    }
    @Test void correctionsAppendAndSavesAreAudited(){
        UUID id=UUID.randomUUID();save(id,article("PUBLISHED",null,false,0,null),true);
        tx.executeWithoutResult(s->repo.correct(id,"Corrected the event date.","owner-sub"));
        assertThat(repo.find(id,false).corrections()).extracting(NewsArticle.Correction::note).containsExactly("Corrected the event date.");
        assertThat(repo.find(id,true).version()).isEqualTo(1);
        assertThat(db.queryForObject("SELECT count(*) FROM news_audit WHERE article_id=?",Integer.class,id)).isEqualTo(2);
    }
    @Test void invalidScheduleRollsBackLeadChanges(){
        UUID first=UUID.randomUUID();save(first,article("PUBLISHED",null,true,0,null),true);
        assertThatThrownBy(()->save(UUID.randomUUID(),article("SCHEDULED",OffsetDateTime.now().minusDays(1),true,0,null),true)).isInstanceOf(ResponseStatusException.class);
        assertThat(repo.find(first,true).isLead()).isTrue();assertThat(repo.list(true)).hasSize(1);
    }
    org.springframework.security.oauth2.jwt.Jwt identity(String token){return org.springframework.security.oauth2.jwt.Jwt.withTokenValue(token).header("alg","RS256").subject("owner").build();}
    @Test void mfaProofIsBoundToTokenExpiresAndRevokes(){
        var directory=org.mockito.Mockito.mock(NewsAdminDirectory.class);
        org.mockito.Mockito.when(directory.active("owner")).thenReturn(true);
        var sessions=new NewsMfaSessions(db,directory);
        String proof=sessions.issue(identity("verified-id-token"),3600);
        var request=new org.springframework.mock.web.MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(NewsMfaSessions.COOKIE,proof));
        assertThat(sessions.valid("verified-id-token",request)).isTrue();
        assertThat(sessions.valid("different-token",request)).isFalse();
        assertThat(sessions.valid("verified-id-token",new org.springframework.mock.web.MockHttpServletRequest())).isFalse();
        assertThat(db.queryForObject("SELECT proof_hash FROM news_mfa_sessions WHERE token_hash IS NOT NULL ORDER BY created_at DESC LIMIT 1",String.class)).isNotEqualTo(proof);
        db.update("UPDATE news_mfa_sessions SET expires_at=now()-interval '1 second'");
        assertThat(sessions.valid("verified-id-token",request)).isFalse();
        String second=sessions.issue(identity("new-token"),3600);request.setCookies(new jakarta.servlet.http.Cookie(NewsMfaSessions.COOKIE,second));
        sessions.revoke(request);assertThat(sessions.valid("new-token",request)).isFalse();
        String revoked=sessions.issue(identity("revoked-token"),3600);request.setCookies(new jakarta.servlet.http.Cookie(NewsMfaSessions.COOKIE,revoked));
        org.mockito.Mockito.when(directory.active("owner")).thenReturn(false);
        assertThat(sessions.valid("revoked-token",request)).isFalse();
        org.mockito.Mockito.when(directory.active("owner")).thenReturn(true);
        assertThat(sessions.valid("revoked-token",request)).isFalse();
    }

    NewsArticle placed(NewsArticle a,String placement){return new NewsArticle(a.id(),a.title(),a.category(),a.summary(),a.body(),a.author(),a.imageId(),a.imageAlt(),a.imageCaption(),a.imageCredit(),a.sources(),a.status(),a.publishAt(),placement.equals("LEAD"),a.version(),a.updatedAt(),a.corrections(),placement,null);}
    @Test void publishedSidebarSlotsAreExplicitAndDraftsDoNotDisplaceThem(){
        UUID first=UUID.randomUUID(),draft=UUID.randomUUID(),second=UUID.randomUUID();
        save(first,placed(article("PUBLISHED",null,false,0,null),"SIDEBAR_1"),true);
        save(draft,placed(article("DRAFT",null,false,0,null),"SIDEBAR_1"),true);
        assertThat(repo.find(first,true).placement()).isEqualTo("SIDEBAR_1");
        save(second,placed(article("PUBLISHED",null,false,0,null),"SIDEBAR_1"),true);
        assertThat(repo.find(first,true).placement()).isEqualTo("MORE");
        assertThat(repo.find(second,true).placement()).isEqualTo("SIDEBAR_1");
        assertThat(repo.find(draft,true).placement()).isEqualTo("SIDEBAR_1");
        assertThat(repo.list(false)).extracting(NewsArticle::placement).containsExactly("SIDEBAR_1","MORE");
    }
    @Test void trashUnpublishesAndRestorePreservesContentAsPrivateDraft(){
        UUID id=UUID.randomUUID(),image=repo.image("image/png",new byte[]{1,2,3});
        save(id,article("PUBLISHED",null,true,0,image),true);
        tx.executeWithoutResult(s->repo.correct(id,"Original correction","owner-sub"));
        var trashed=tx.execute(s->repo.trash(id,1,"owner-sub",false));
        assertThat(trashed.deletedAt()).isNotNull();assertThat(repo.list(true)).isEmpty();assertThat(repo.list(true,true)).hasSize(1);assertThat(repo.list(false)).isEmpty();
        assertThatThrownBy(()->repo.image(image,false)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->tx.execute(s->repo.trash(id,1,"owner-sub",true))).isInstanceOf(ResponseStatusException.class);
        var restored=tx.execute(s->repo.trash(id,2,"owner-sub",true));
        assertThat(restored.deletedAt()).isNull();assertThat(restored.status()).isEqualTo("DRAFT");assertThat(restored.placement()).isEqualTo("MORE");assertThat(restored.body()).isEqualTo("Article body");assertThat(restored.corrections()).hasSize(1);
        assertThat(repo.list(false)).isEmpty();assertThat(repo.list(true,true)).isEmpty();assertThat(repo.list(true)).hasSize(1);
    }

}
