package app.sanctuary.api.news;

import java.util.*;
import java.time.Instant;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sql.DataSource;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.sanctuary.api.auth.service.WebAuthCookieService;
import app.sanctuary.api.config.*;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

/** Real HTTP/security/controller/transaction/PostgreSQL lifecycle; Cognito and JWT decoding are substituted. */
@EnabledIfEnvironmentVariable(named="NEWS_TEST_DB",matches="jdbc:postgresql://127\\.0\\.0\\.1:15439/news_cms_test")
class NewsHttpLifecycleIntegrationTest {
    static DataSource testDataSource;
    @Configuration @EnableWebMvc @EnableWebSecurity @EnableTransactionManagement @Import({NewsSecurityConfig.class,WebConfig.class})
    static class Config {
        @Bean @Primary WebProperties web(){var web=new WebProperties();web.setAllowedOrigins(List.of("https://admin.example.org"));return web;}
        @Bean DataSource datasource(){return testDataSource;}
        @Bean JdbcTemplate jdbc(DataSource ds){return new JdbcTemplate(ds);}
        @Bean PlatformTransactionManager transactions(DataSource ds){return new DataSourceTransactionManager(ds);}
        @Bean NewsRepository repository(JdbcTemplate db){return new NewsRepository(db);}
        @Bean NewsMfaSessions sessions(JdbcTemplate db){return new NewsMfaSessions(db, directory());}
        @Bean NewsAdminDirectory directory(){var directory=mock(NewsAdminDirectory.class);when(directory.active(anyString())).thenReturn(true);return directory;}
        @Bean NewsAuthStateStore pending(JdbcTemplate db,AuthProperties auth){return new NewsAuthStateStore(db,"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",new org.springframework.mock.env.MockEnvironment(),auth);}
        @Bean AuthProperties auth(){return new AuthProperties(true,"test-client","test-pool","test-client");}
        @Bean CookieBearerTokenResolver resolver(){return new CookieBearerTokenResolver();}
        @Bean WebAuthCookieService cookies(AuthProperties auth){return new WebAuthCookieService(auth);}
        @Bean JwtDecoder decoder(){return token->{
            if(!"audit-id-token".equals(token))throw new BadJwtException("Invalid audit token");
            return Jwt.withTokenValue(token).header("alg","RS256").subject("audit-admin").claim("token_use","id").audience(List.of("test-client")).expiresAt(Instant.now().plusSeconds(3600)).claim("cognito:groups",List.of("news-admin")).build();
        };}
        @Bean CognitoIdentityProviderClient cognito(){
            var client=mock(CognitoIdentityProviderClient.class);
            when(client.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder().challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA).challengeParameters(Map.of("USER_ID_FOR_SRP","audit-admin")).session("audit-session").build());
            when(client.respondToAuthChallenge(any(RespondToAuthChallengeRequest.class))).thenReturn(RespondToAuthChallengeResponse.builder().authenticationResult(AuthenticationResultType.builder().idToken("audit-id-token").accessToken("audit-access-token").expiresIn(3600).build()).build());
            return client;
        }
        @Bean NewsController controller(NewsRepository repo){return new NewsController(repo);}
        @Bean NewsMfaAuthController authentication(CognitoIdentityProviderClient c,AuthProperties a,ObjectProvider<JwtDecoder> d,NewsMfaSessions s,WebAuthCookieService cookies,NewsAuthStateStore pending){return new NewsMfaAuthController(c,a,d,s,cookies,pending);}
        @Bean @Order(2) SecurityFilterChain publicRoutes(HttpSecurity http)throws Exception{return http.cors(org.springframework.security.config.Customizer.withDefaults()).csrf(csrf->csrf.disable()).authorizeHttpRequests(r->r.anyRequest().permitAll()).build();}
    }
    @Test void completePublishingLifecycleThroughProtectedHttpEndpoints()throws Exception{
        var base=new DriverManagerDataSource(System.getenv("NEWS_TEST_DB"),"postgres","local-cms-test");
        new JdbcTemplate(base).execute("DROP SCHEMA IF EXISTS cms_http_audit CASCADE");new JdbcTemplate(base).execute("CREATE SCHEMA cms_http_audit");
        testDataSource=new DriverManagerDataSource(System.getenv("NEWS_TEST_DB")+"?currentSchema=cms_http_audit","postgres","local-cms-test");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V16__create_news_cms.sql"),new ClassPathResource("db/migration/V17__create_news_mfa_sessions.sql"),new ClassPathResource("db/migration/V18__news_placement_and_trash.sql"),new ClassPathResource("db/migration/V19__harden_news_auth.sql")).execute(testDataSource);
        try(var context=new AnnotationConfigWebApplicationContext()){
            context.setServletContext(new MockServletContext());context.register(Config.class);context.refresh();
            MockMvc mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(new NewsRequestLimitsFilter(),context.getBean("springSecurityFilterChain",Filter.class)).build();
            ObjectMapper json=new ObjectMapper().findAndRegisterModules();
            mvc.perform(get("/admin/news")).andExpect(status().isUnauthorized());
            mvc.perform(get("/admin/news").cookie(new Cookie("sanctuary_id","audit-id-token"))).andExpect(status().isForbidden());
            mvc.perform(post("/auth/news/login").header("Origin","https://evil.example.org").header("X-Sanctuary-News-Admin","1").contentType("application/json").content("{}" )).andExpect(status().isForbidden());
            mvc.perform(post("/auth/news/login").header("X-Sanctuary-News-Admin","1").contentType("application/json").content("X".repeat(16385))).andExpect(status().isPayloadTooLarge());
            var login=mvc.perform(post("/auth/news/login").header("X-Sanctuary-News-Admin","1").contentType("application/json").content("{\"email\":\"audit@example.org\",\"password\":\"audit-password\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CODE")).andReturn();
            String challenge=json.readTree(login.getResponse().getContentAsString()).get("challengeId").asText();
            var verified=mvc.perform(post("/auth/news/verify").header("X-Sanctuary-News-Admin","1").contentType("application/json").content(json.writeValueAsString(Map.of("challengeId",challenge,"code","123456")))).andExpect(status().isOk()).andExpect(jsonPath("$.state").value("AUTHENTICATED")).andReturn();
            Cookie[] cookies=verified.getResponse().getCookies();
            mvc.perform(get("/admin/news").cookie(cookies)).andExpect(status().isOk());
            mvc.perform(post("/admin/news").cookie(cookies)).andExpect(status().isForbidden());
            var pixels=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",pixels);
            var uploaded=mvc.perform(multipart("/admin/news/images").file(new MockMultipartFile("file","test.png","image/png",pixels.toByteArray())).cookie(cookies).header("X-Sanctuary-News-Admin","1")).andExpect(status().isOk()).andReturn();
            String image=json.readTree(uploaded.getResponse().getContentAsString()).get("id").asText();
            Map<String,Object> article=new LinkedHashMap<>();article.put("title","Isolated HTTP audit story");article.put("category","Vatican");article.put("summary","Audit summary");article.put("body","Audit story body");article.put("author","Audit reporter");article.put("imageId",image);article.put("imageAlt","Audit image");article.put("imageCaption","Audit caption");article.put("imageCredit","Audit-owned image");article.put("sources","https://example.org");article.put("status","DRAFT");article.put("placement","LEAD");article.put("version",0);
            Map<String,Object> invalid=new LinkedHashMap<>(article);invalid.put("body","PRIVATE_AUDIT_TEXT".repeat(10000));
            mvc.perform(post("/admin/news").cookie(cookies).header("X-Sanctuary-News-Admin","1").contentType("application/json").content(json.writeValueAsString(invalid))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("body"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE_AUDIT_TEXT"))));
            var draft=mvc.perform(post("/admin/news").cookie(cookies).header("X-Sanctuary-News-Admin","1").contentType("application/json").content(json.writeValueAsString(article))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT")).andReturn();
            String id=json.readTree(draft.getResponse().getContentAsString()).get("id").asText();
            mvc.perform(get("/content/news")).andExpect(jsonPath("$.length()").value(0));
            mvc.perform(get("/content/news/"+id)).andExpect(status().isNotFound());
            mvc.perform(get("/content/news/images/"+image)).andExpect(status().isNotFound());
            article.put("status","PUBLISHED");
            mvc.perform(put("/admin/news/"+id).cookie(cookies).header("X-Sanctuary-News-Admin","1").contentType("application/json").content(json.writeValueAsString(article))).andExpect(status().isOk()).andExpect(jsonPath("$.placement").value("LEAD")).andExpect(jsonPath("$.version").value(1));
            mvc.perform(get("/content/news/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.body").value("Audit story body"));
            mvc.perform(get("/content/news/images/"+image)).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
            mvc.perform(post("/admin/news/"+id+"/corrections").cookie(cookies).header("X-Sanctuary-News-Admin","1").contentType("application/json").content("{\"note\":\"Audit correction\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
            mvc.perform(delete("/admin/news/"+id+"?version=2").cookie(cookies).header("X-Sanctuary-News-Admin","1")).andExpect(status().isOk());
            mvc.perform(get("/admin/news?trash=true").cookie(cookies)).andExpect(jsonPath("$.length()").value(1));
            mvc.perform(get("/content/news/"+id)).andExpect(status().isNotFound());
            mvc.perform(get("/content/news/images/"+image)).andExpect(status().isNotFound());
            mvc.perform(post("/admin/news/"+id+"/restore?version=3").cookie(cookies).header("X-Sanctuary-News-Admin","1")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT")).andExpect(jsonPath("$.placement").value("MORE")).andExpect(jsonPath("$.corrections[0].note").value("Audit correction"));
            mvc.perform(get("/content/news")).andExpect(jsonPath("$.length()").value(0));
            mvc.perform(post("/admin/news/logout").cookie(cookies).header("X-Sanctuary-News-Admin","1")).andExpect(status().isNoContent());
            mvc.perform(get("/admin/news").cookie(cookies)).andExpect(status().isForbidden());
        }
    }
}
