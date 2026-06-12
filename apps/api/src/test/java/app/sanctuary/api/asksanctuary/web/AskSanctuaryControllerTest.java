package app.sanctuary.api.asksanctuary.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryGuardrailDto;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryRequest;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryStatusResponse;
import app.sanctuary.api.asksanctuary.dto.ScriptureReferenceDto;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryResult;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryStatus;
import app.sanctuary.api.asksanctuary.service.AskSanctuaryService;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@ExtendWith(MockitoExtension.class)
class AskSanctuaryControllerTest {

    @Mock
    private AskSanctuaryService service;

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private HttpServletRequest servletRequest;

    @Test
    void signedOutRequestReturnsAccountRequiredPayload() {
        AskSanctuaryController controller = new AskSanctuaryController(service, userAccountService);

        var response = controller.ask(null, servletRequest, new AskSanctuaryRequest("I have a job interview tomorrow."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requiresAccount()).isTrue();
        assertThat(response.getBody().redirectAction()).isEqualTo("SIGN_IN");
        verify(userAccountService, never()).ensureAccount(any());
        verify(service, never()).answer(any(), any(), any());
    }

    @Test
    void signedOutStatusReturnsForbidden() {
        AskSanctuaryController controller = new AskSanctuaryController(service, userAccountService);

        var response = controller.status(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo(new AskSanctuaryStatusResponse("v1", false));
        verify(userAccountService, never()).ensureAccount(any());
    }

    @Test
    void authenticatedStatusEnsuresAccountThenReturnsServiceStatus() {
        AskSanctuaryController controller = new AskSanctuaryController(service, userAccountService);
        UUID userId = UUID.randomUUID();
        when(userAccountService.ensureAccount(any(CurrentUser.class))).thenReturn(account(userId));
        when(service.status(userId)).thenReturn(new AskSanctuaryStatusResponse("v1", true));

        var response = controller.status(authentication());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new AskSanctuaryStatusResponse("v1", true));
    }

    @Test
    void authenticatedDisclaimerAcceptancePersistsConsent() {
        AskSanctuaryController controller = new AskSanctuaryController(service, userAccountService);
        UUID userId = UUID.randomUUID();
        when(userAccountService.ensureAccount(any(CurrentUser.class))).thenReturn(account(userId));
        when(service.acceptDisclaimer(userId)).thenReturn(new AskSanctuaryStatusResponse("v1", true));

        var response = controller.acceptDisclaimer(authentication());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new AskSanctuaryStatusResponse("v1", true));
    }

    @Test
    void authenticatedRequestEnsuresAccountThenCallsService() {
        AskSanctuaryController controller = new AskSanctuaryController(service, userAccountService);
        UUID userId = UUID.randomUUID();
        AskSanctuaryResponse payload = normalPayload();
        when(userAccountService.ensureAccount(any(CurrentUser.class)))
            .thenReturn(account(userId));
        when(servletRequest.getHeader("X-Forwarded-For")).thenReturn("203.0.113.10, 10.0.0.1");
        when(service.answer(eq(userId), eq("I have a job interview tomorrow."), any()))
            .thenReturn(new AskSanctuaryResult(
                AskSanctuaryStatus.OK,
                AskSanctuaryIntent.WORK_OR_DISCERNMENT,
                AskSanctuaryGuardrailType.NONE,
                false,
                payload
            ));

        var response = controller.ask(authentication(), servletRequest, new AskSanctuaryRequest("I have a job interview tomorrow."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(payload);
        verify(userAccountService).ensureAccount(any(CurrentUser.class));
        verify(service).answer(eq(userId), eq("I have a job interview tomorrow."), any());
    }

    private JwtAuthenticationToken authentication() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("cognito-sub")
            .claim("email", "user@example.com")
            .claim("given_name", "Pat")
            .claim("family_name", "User")
            .build();
        return new JwtAuthenticationToken(jwt, java.util.List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private UserAccountDto account(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new UserAccountDto(
            userId,
            "cognito-sub",
            "user@example.com",
            "Pat",
            "User",
            "Pat User",
            "en",
            null,
            now,
            now
        );
    }

    private AskSanctuaryResponse normalPayload() {
        return new AskSanctuaryResponse(
            "OK",
            false,
            false,
            null,
            null,
            "Courage for today",
            new ScriptureReferenceDto("Isaiah", "41", "10"),
            new ScriptureReferenceDto("Matthew", "11", "28"),
            "St. Joseph",
            "Our Father",
            "Bring the day to prayer.",
            "Pray slowly for one minute.",
            "WORK_OR_DISCERNMENT",
            new AskSanctuaryGuardrailDto("NONE", false)
        );
    }
}
