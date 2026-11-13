package app.sanctuary.api.admin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"sanctuary.auth.enabled=true","sanctuary.admin.enabled=false","sanctuary.push.enabled=false"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL",matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class AnonymousPushSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate db;
    @MockitoBean JwtDecoder decoder;
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url",()->System.getenv("ADMIN_TEST_DB_URL"));
        registry.add("spring.datasource.username",()->"sanctuary_test");registry.add("spring.datasource.password",()->"sanctuary_test");
    }
    @Test void onlyInstallationRegistrationIsPublicAndCannotAttachAnAccount()throws Exception {
        db.update("DELETE FROM push_registration_limits");
        UUID id=UUID.randomUUID();
        String body="{\"installationId\":\""+id+"\",\"token\":\"synthetic-public-token-"+id+"\",\"platform\":\"ios\",\"appVersion\":\"1.0.17\",\"enabled\":true,\"installationSecret\":\""+"E".repeat(64)+"\",\"revision\":1,\"user_id\":\"spoofed-owner\"}";
        mvc.perform(put("/push/installations").contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.registered").value(true));
        assertNull(db.queryForObject("SELECT user_id FROM push_devices WHERE installation_id=?",UUID.class,id));
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/push/installations")).andExpect(status().is4xxClientError());
        mvc.perform(get("/admin-api/messages")).andExpect(status().isForbidden());
        db.update("DELETE FROM push_devices WHERE installation_id=?",id);
    }
}
