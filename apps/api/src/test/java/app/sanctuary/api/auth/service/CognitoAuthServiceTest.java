package app.sanctuary.api.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse;

import app.sanctuary.api.auth.dto.AuthRegisterRequest;
import app.sanctuary.api.auth.dto.AuthResetPasswordRequest;
import app.sanctuary.api.config.AuthAbuseProtectionProperties;
import app.sanctuary.api.config.AuthProperties;

class CognitoAuthServiceTest {

    @Test
    void rejectsBlockedSignupEmailDomainsBeforeCallingCognito() {
        CognitoAuthService service = new CognitoAuthService(
            mock(CognitoIdentityProviderClient.class),
            new AuthProperties(true, "client-id", "pool-id", "client-id"),
            new AuthAbuseProtectionProperties()
        );

        AuthRegisterRequest request = new AuthRegisterRequest(
            "John",
            "Doe",
            "john.doe@example.com",
            "Password1234"
        );

        assertThatThrownBy(() -> service.register(request))
            .isInstanceOf(BlockedEmailDomainException.class)
            .hasMessageContaining("real email");
    }

    @Test
    void resetPasswordReturnsSignedInSessionAfterConfirmingNewPassword() {
        CognitoIdentityProviderClient cognito = mock(CognitoIdentityProviderClient.class);
        when(cognito.confirmForgotPassword(any(ConfirmForgotPasswordRequest.class)))
            .thenReturn(ConfirmForgotPasswordResponse.builder().build());
        when(cognito.initiateAuth(any(InitiateAuthRequest.class)))
            .thenReturn(InitiateAuthResponse.builder()
                .authenticationResult(AuthenticationResultType.builder()
                    .accessToken("access-token")
                    .idToken(jwtPayload("""
                        {"email":"jane@example.com","name":"Jane Doe"}
                        """))
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .build())
                .build());

        CognitoAuthService service = new CognitoAuthService(
            cognito,
            new AuthProperties(true, "client-id", "pool-id", "client-id"),
            new AuthAbuseProtectionProperties()
        );

        var response = service.resetPassword(new AuthResetPasswordRequest(
            "Jane@Example.com",
            "123456",
            "NewPassword123"
        ));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.displayName()).isEqualTo("Jane Doe");
        verify(cognito).confirmForgotPassword(any(ConfirmForgotPasswordRequest.class));
        verify(cognito).initiateAuth(any(InitiateAuthRequest.class));
    }

    private String jwtPayload(String payload) {
        return "header."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
            + ".signature";
    }
}
