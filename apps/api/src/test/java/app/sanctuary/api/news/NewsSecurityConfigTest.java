package app.sanctuary.api.news;

import java.util.*;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import app.sanctuary.api.config.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NewsSecurityConfigTest {
    @Configuration @EnableWebSecurity @EnableWebMvc @Import({NewsSecurityConfig.class,WebConfig.class})
    static class Config {
        @Bean AuthProperties auth(@Value("${test.auth:true}") boolean enabled) { return new AuthProperties(enabled,"client","pool","client"); }
        @Bean @Primary WebProperties web(){var web=new WebProperties();web.setAllowedOrigins(List.of("https://admin.example.org"));return web;}
        @Bean CookieBearerTokenResolver resolver() { return new CookieBearerTokenResolver(); }
        @Bean JwtDecoder decoder() { return value -> {
            if (value.equals("invalid")) throw new BadJwtException("invalid signature");
            return Jwt.withTokenValue(value).header("alg","RS256").subject("test-user").claim("token_use","id").audience(List.of("client")).expiresAt(java.time.Instant.now().plusSeconds(3600))
                .claim("cognito:groups",value.equals("admin")?List.of("news-admin"):List.of("app-admin")).build();
        }; }
        @Bean NewsMfaSessions sessions(){return new NewsMfaSessions(null,null){
            @Override public boolean valid(String token,jakarta.servlet.http.HttpServletRequest request){return "verified".equals(request.getHeader("X-Test-Mfa"));}
        };}
        @Bean TestController controller() { return new TestController(); }
    }
    @RestController static class TestController {
        @GetMapping("/admin/news") String list(){return "private";}
        @PostMapping("/admin/news") String save(){return "saved";}
    }
    private void check(boolean enabled, java.util.function.Consumer<MockMvc> assertions) {
        try(var context=new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.setEnvironment(new org.springframework.mock.env.MockEnvironment()
                .withProperty("test.auth",Boolean.toString(enabled)));
            context.register(Config.class);context.refresh();
            assertions.accept(MockMvcBuilders.webAppContextSetup(context).addFilters(context.getBean("springSecurityFilterChain",Filter.class)).build());
        }
    }
    @Test void anonymousAndInvalidTokensCannotRead() {check(true,mvc->{try {
        mvc.perform(get("/admin/news")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/news").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
    }catch(Exception e){throw new RuntimeException(e);}});}
    @Test void ordinaryAndAppAdminsCannotReadOrWrite(){check(true,mvc->{try{
        mvc.perform(get("/admin/news").header("Authorization","Bearer reader")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/news").header("Authorization","Bearer reader").header("X-Sanctuary-News-Admin","1")).andExpect(status().isForbidden());
    }catch(Exception e){throw new RuntimeException(e);}});}
    @Test void newsAdminRequiresMutationHeader(){check(true,mvc->{try{
        mvc.perform(get("/admin/news").header("Authorization","Bearer admin").header("X-Test-Mfa","verified")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-cache, no-store, max-age=0, must-revalidate"));
        mvc.perform(get("/admin/news").cookie(new jakarta.servlet.http.Cookie("sanctuary_id","admin")).header("X-Test-Mfa","verified")).andExpect(status().isOk());
        mvc.perform(post("/admin/news").cookie(new jakarta.servlet.http.Cookie("sanctuary_id","admin"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/news").header("Authorization","Bearer admin")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/news").header("Authorization","Bearer admin").header("X-Sanctuary-News-Admin","1").header("X-Test-Mfa","verified")).andExpect(status().isOk());
    }catch(Exception e){throw new RuntimeException(e);}});}
    @Test void passwordOnlyAdminCannotReadOrWrite(){check(true,mvc->{try{
        mvc.perform(get("/admin/news").header("Authorization","Bearer admin")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/news").header("Authorization","Bearer admin").header("X-Sanctuary-News-Admin","1")).andExpect(status().isForbidden());
    }catch(Exception e){throw new RuntimeException(e);}});}
    @Test void corsRejectsHostileOriginsIncludingCredentialedMutations(){check(true,mvc->{try{
        mvc.perform(options("/admin/news").header("Origin","https://evil.example.org").header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","X-Sanctuary-News-Admin")).andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        mvc.perform(post("/admin/news").header("Origin","https://evil.example.org").header("Authorization","Bearer admin").header("X-Test-Mfa","verified").header("X-Sanctuary-News-Admin","1")).andExpect(status().isForbidden());
        mvc.perform(options("/admin/news").header("Origin","https://admin.example.org").header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","X-Sanctuary-News-Admin")).andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","https://admin.example.org")).andExpect(header().string("Access-Control-Allow-Credentials","true"));
    }catch(Exception e){throw new RuntimeException(e);}});}
    @Test void disabledAuthenticationNeverUnlocksAdmin(){check(false,mvc->{try{
        mvc.perform(get("/admin/apps")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/news").header("Authorization","Bearer admin").header("X-Test-Mfa","verified")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/news").header("Authorization","Bearer admin").header("X-Sanctuary-News-Admin","1").header("X-Test-Mfa","verified")).andExpect(status().isForbidden());
    }catch(Exception e){throw new RuntimeException(e);}});}
    @Test void newsRoleDoesNotGrantFutureAppManagementAccess(){check(true,mvc->{try{
        mvc.perform(get("/admin/apps").header("Authorization","Bearer admin").header("X-Test-Mfa","verified")).andExpect(status().isForbidden());
    }catch(Exception e){throw new RuntimeException(e);}});}
}
