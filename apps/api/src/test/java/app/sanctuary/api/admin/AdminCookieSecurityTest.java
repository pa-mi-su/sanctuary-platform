package app.sanctuary.api.admin;

import java.util.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.sanctuary.api.auth.service.AuthFlowException;
import app.sanctuary.api.config.ApiExceptionHandler;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class AdminCookieSecurityTest {
    AdminAuthService auth; AdminUsersService users; MockMvc mvc;
    final String token="A".repeat(43);
    @BeforeEach void setup() {
        auth=mock(AdminAuthService.class); users=mock(AdminUsersService.class);
        var cookies=new AdminSessionCookies(true);
        when(auth.authenticate(isNull())).thenThrow(new AuthFlowException(HttpStatus.UNAUTHORIZED,"No admin session"));
        when(auth.authenticate("Bearer "+token)).thenReturn("owner-sub");
        mvc=MockMvcBuilders.standaloneSetup(new AdminController(auth,mock(AdminContentService.class),users,cookies))
            .setControllerAdvice(new ApiExceptionHandler()).addFilters(new AdminSessionFilter(auth,new ObjectMapper(),cookies)).build();
    }
    @Test void mfaVerificationIssuesScopedHttpOnlyCookieWithoutReturningTokenToNewClient() throws Exception {
        when(auth.verify("challenge","123456")).thenReturn(Map.of("token",token,"email","owner@example.test"));
        when(auth.sessionInfo("Bearer "+token)).thenReturn(Map.of("email","owner@example.test","expiresAt","2026-12-01T00:00:00Z","idleHours",8,"maximumHours",24));
        mvc.perform(post("/admin-api/verify").header("X-Sanctuary-Admin","1").header("X-Sanctuary-Admin-Client","cookie-v2").contentType("application/json").content("{\"challenge\":\"challenge\",\"code\":\"123456\"}"))
            .andExpect(status().isOk()).andExpect(header().string("Set-Cookie",allOf(containsString("HttpOnly"),containsString("Secure"),containsString("SameSite=Strict"),containsString("Path=/admin-api"),containsString("Max-Age=86400"))))
            .andExpect(jsonPath("$.token").doesNotExist()).andExpect(jsonPath("$.idleHours").value(8));
    }
    @Test void cookieIsForwardedToControllersOnlyAfterAdminFilterChecks() throws Exception {
        when(users.list("",0,false)).thenReturn(Map.of("items",List.of(),"hasMore",false));
        mvc.perform(get("/admin-api/users").header("X-Sanctuary-Admin","1").cookie(new Cookie(AdminSessionCookies.NAME,token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        verify(auth,never()).authenticate(isNull()); verify(users).list("",0,false);
    }
    @Test void ambientCookieCannotAuthorizeSimpleCrossSiteRequestWithoutCustomHeader() throws Exception {
        mvc.perform(get("/admin-api/users").cookie(new Cookie(AdminSessionCookies.NAME,token))).andExpect(status().isForbidden());
        verifyNoInteractions(auth,users);
    }
    @Test void signoutClearsCookieAndRevokesServerSession() throws Exception {
        mvc.perform(post("/admin-api/logout").header("X-Sanctuary-Admin","1").cookie(new Cookie(AdminSessionCookies.NAME,token)))
            .andExpect(status().isOk()).andExpect(header().string("Set-Cookie",containsString("Max-Age=0")));
        verify(auth).logout("Bearer "+token);
    }
    @Test void oldAlreadyLoadedClientCanFinishItsBearerLoginDuringRollingRelease() throws Exception {
        when(auth.verify("challenge","123456")).thenReturn(Map.of("token",token,"email","owner@example.test"));
        mvc.perform(post("/admin-api/verify").header("X-Sanctuary-Admin","1").contentType("application/json").content("{\"challenge\":\"challenge\",\"code\":\"123456\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").value(token));
    }
}
