package app.sanctuary.api.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"sanctuary.auth.enabled=false", "sanctuary.admin.enabled=false"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="ADMIN_TEST_DB_URL", matches="jdbc:postgresql://(localhost|127\\.0\\.0\\.1):[0-9]+/.*")
class AdminSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("ADMIN_TEST_DB_URL"));
        registry.add("spring.datasource.username", () -> "sanctuary_test");
        registry.add("spring.datasource.password", () -> "sanctuary_test");
    }
    @Test void disablingPublicAuthCannotOpenAdminRoutes() throws Exception {
        mvc.perform(get("/admin-api/users").header("X-Sanctuary-Admin", "1"))
            .andExpect(status().isServiceUnavailable()).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(post("/admin-api/login").header("X-Sanctuary-Admin", "1").contentType("application/json")
            .content("{\"email\":\"fixture@example.org\",\"password\":\"password\"}"))
            .andExpect(status().isServiceUnavailable());
    }
    @Test void cookieOrOrdinaryTokenCannotBypassAdminFilter() throws Exception {
        mvc.perform(get("/admin-api/users").header("Authorization", "Bearer ordinary.cognito.jwt"))
            .andExpect(status().isForbidden());
    }
    @Test void preflightDoesNotRevealPrivateData() throws Exception {
        mvc.perform(options("/admin-api/users").header("Origin", "http://localhost:4200")
            .header("Access-Control-Request-Method", "GET").header("Access-Control-Request-Headers", "authorization,x-sanctuary-admin"))
            .andExpect(status().isOk());
        mvc.perform(options("/admin-api/users").header("Origin", "https://untrusted.example")
            .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }
}
