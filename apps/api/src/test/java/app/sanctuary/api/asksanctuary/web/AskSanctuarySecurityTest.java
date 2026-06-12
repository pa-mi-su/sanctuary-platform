package app.sanctuary.api.asksanctuary.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import app.sanctuary.api.asksanctuary.service.AskSanctuaryService;
import app.sanctuary.api.config.CookieBearerTokenResolver;
import app.sanctuary.api.config.SecurityConfig;
import app.sanctuary.api.user.service.UserAccountService;

@WebMvcTest(AskSanctuaryController.class)
@Import({SecurityConfig.class, CookieBearerTokenResolver.class})
class AskSanctuarySecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AskSanctuaryService askSanctuaryService;

    @MockitoBean
    private UserAccountService userAccountService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void statusSubpathReachesControllerWhenSignedOut() throws Exception {
        mockMvc.perform(get("/api/ask-sanctuary/status"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.disclaimerVersion").value("v1"))
            .andExpect(jsonPath("$.disclaimerAccepted").value(false))
            .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void disclaimerSubpathReachesControllerWhenSignedOut() throws Exception {
        mockMvc.perform(post("/api/ask-sanctuary/disclaimer"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.disclaimerVersion").value("v1"))
            .andExpect(jsonPath("$.disclaimerAccepted").value(false))
            .andExpect(jsonPath("$.available").value(true));
    }
}
