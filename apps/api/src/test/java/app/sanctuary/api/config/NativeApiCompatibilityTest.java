package app.sanctuary.api.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import app.sanctuary.api.auth.dto.AuthSessionResponse;
import app.sanctuary.api.auth.service.CognitoAuthService;
import app.sanctuary.api.auth.service.WebAuthCookieService;
import app.sanctuary.api.auth.web.AuthController;
import app.sanctuary.api.news.*;
import app.sanctuary.api.user.dto.*;
import app.sanctuary.api.user.service.*;
import app.sanctuary.api.user.web.UserProgressController;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Both production security chains, real native controllers/JSON and RSA validation.
 * Service persistence/Cognito are mocked; this never signs in or deletes a real account. */
class NativeApiCompatibilityTest {
    static KeyPair keys;
    AnnotationConfigWebApplicationContext context;
    MockMvc mvc;

    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, NewsSecurityConfig.class, WebConfig.class})
    static class Config {
        @Bean @Primary AuthProperties auth() { return new AuthProperties(true,"client","pool","client"); }
        @Bean @Primary WebProperties web() { return new WebProperties(); }
        @Bean CookieBearerTokenResolver resolver() { return new CookieBearerTokenResolver(); }
        @Bean NewsMfaSessions sessions() { return mock(NewsMfaSessions.class); }
        @Bean JwtDecoder decoder() {
            var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
            decoder.setJwtValidator(CognitoTokenValidators.forClient("https://issuer.example.org/pool", "client"));
            return decoder;
        }
        @Bean CognitoAuthService cognito() { return mock(CognitoAuthService.class); }
        @Bean UserProfileService profiles() { return mock(UserProfileService.class); }
        @Bean UserProgressService progress() { return mock(UserProgressService.class); }
        @Bean UserAccountService accounts() { return mock(UserAccountService.class); }
        @Bean AuthController authController(CognitoAuthService service, AuthProperties auth) {
            return new AuthController(service, new WebAuthCookieService(auth));
        }
        @Bean UserProgressController users(UserProgressService progress, UserAccountService accounts, UserProfileService profiles) {
            return new UserProgressController(progress, accounts, profiles);
        }
    }

    @BeforeAll static void createKeys() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); keys = generator.generateKeyPair();
    }
    @BeforeEach void setup() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext()); context.register(Config.class); context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .addFilters(new NewsRequestLimitsFilter(), context.getBean("springSecurityFilterChain", Filter.class)).build();
        when(context.getBean(UserProfileService.class).getProfile(any())).thenReturn(new UserProfileDto(
            "mobile-sub","reader@example.org","Mobile","Reader","Mobile Reader","en",null,"America/New_York",
            true,true,false,true,1,2,3,4,5));
        when(context.getBean(UserAccountService.class).ensureAccount(any())).thenReturn(new UserAccountDto(
            UUID.randomUUID(),"mobile-sub","reader@example.org","Mobile","Reader","Mobile Reader","en",null,null,null));
    }
    @AfterEach void close() { context.close(); }

    String token(String use) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer("https://issuer.example.org/pool").subject("mobile-sub")
            .expirationTime(Date.from(Instant.now().plusSeconds(300))).claim("token_use",use)
            .claim("email","reader@example.org");
        if (use.equals("id")) claims.audience("client"); else claims.claim("client_id","client");
        var signed = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),claims.build());
        signed.sign(new RSASSASigner((RSAPrivateKey)keys.getPrivate())); return signed.serialize();
    }

    @Test void mobileBearerTokensReadProfileWithoutAdminGroupCookiesOrMfa() throws Exception {
        for (String use : List.of("id","access")) {
            mvc.perform(get("/me").header("Authorization","Bearer " + token(use)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value("mobile-sub"))
                .andExpect(jsonPath("$.activeNovenaCount").value(4));
            mvc.perform(get("/admin/news").header("Authorization","Bearer " + token(use)))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(context.getBean(NewsMfaSessions.class));
    }

    @Test void mobileProgressWritesAndAccountDeletionDoNotRequireAdminHeaderOrMfa() throws Exception {
        String bearer = "Bearer " + token("id");
        mvc.perform(put("/me/favorites/SAINT/example").header("Authorization",bearer)).andExpect(status().isNoContent());
        mvc.perform(delete("/me/favorites/SAINT/example").header("Authorization",bearer)).andExpect(status().isNoContent());
        mvc.perform(get("/me/novena-commitments").header("Authorization",bearer)).andExpect(status().isOk());
        mvc.perform(delete("/me/novena-commitments/example").header("Authorization",bearer)).andExpect(status().isNoContent());
        mvc.perform(delete("/me").header("Authorization",bearer)).andExpect(status().isNoContent());
        verify(context.getBean(UserAccountService.class)).deleteAccount(argThat(user -> "mobile-sub".equals(user.cognitoSub())));
        verifyNoInteractions(context.getBean(NewsMfaSessions.class));
    }

    @Test void nativeLoginAndRefreshKeepJsonTokensAndDoNotIssueAdminCookies() throws Exception {
        var session = new AuthSessionResponse("access","id","refresh","Bearer",3600,"reader@example.org","Mobile Reader");
        var service = context.getBean(CognitoAuthService.class);
        when(service.login(any())).thenReturn(session); when(service.refresh(any())).thenReturn(session);
        mvc.perform(post("/auth/login").contentType("application/json")
                .content("{\"email\":\"reader@example.org\",\"password\":\"Password1234\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.idToken").value("id"))
            .andExpect(jsonPath("$.accessToken").value("access")).andExpect(jsonPath("$.refreshToken").value("refresh"))
            .andExpect(jsonPath("$.expiresIn").value(3600)).andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(post("/auth/refresh").contentType("application/json").content("{\"refreshToken\":\"refresh\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.idToken").value("id"))
            .andExpect(jsonPath("$.refreshToken").value("refresh")).andExpect(header().doesNotExist("Set-Cookie"));
        verifyNoInteractions(context.getBean(NewsMfaSessions.class));
    }

    @Test void nativeProtectedRoutesStillRejectMissingOrInvalidTokens() throws Exception {
        mvc.perform(get("/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/me").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        verifyNoInteractions(context.getBean(UserProfileService.class));
    }
}
