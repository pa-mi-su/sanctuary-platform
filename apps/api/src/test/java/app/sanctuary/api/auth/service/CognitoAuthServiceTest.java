package app.sanctuary.api.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import app.sanctuary.api.auth.dto.AuthRegisterRequest;
import app.sanctuary.api.config.AuthAbuseProtectionProperties;
import app.sanctuary.api.config.AuthProperties;

class CognitoAuthServiceTest {

    @Test
    void rejectsBlockedSignupEmailDomainsBeforeCallingCognito() {
        CognitoAuthService service = new CognitoAuthService(
            CognitoIdentityProviderClient.builder().build(),
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
}
