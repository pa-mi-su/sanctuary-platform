package app.sanctuary.api.news;

import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.server.ResponseStatusException;
import app.sanctuary.api.auth.service.WebAuthCookieService;
import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NewsMfaAuthControllerTest {
    CognitoIdentityProviderClient cognito;NewsMfaSessions sessions;NewsMfaAuthController controller;
    AuthProperties auth;JwtDecoder decoder;NewsAuthStateStore pending;
    @BeforeEach @SuppressWarnings("unchecked") void setup(){
        cognito=mock(CognitoIdentityProviderClient.class);sessions=mock(NewsMfaSessions.class);decoder=mock(JwtDecoder.class);
        ObjectProvider<JwtDecoder> provider=mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(decoder);
        pending=mock(NewsAuthStateStore.class);
        var states=new java.util.concurrent.ConcurrentHashMap<String,NewsAuthStateStore.Pending>();
        when(pending.allowLogin(anyString())).thenReturn(true);
        doAnswer(invocation->{states.put(invocation.getArgument(0),invocation.getArgument(1));return null;}).when(pending).put(anyString(),any());
        when(pending.take(anyString())).thenAnswer(invocation->states.remove(invocation.getArgument(0)));
        auth=new AuthProperties(true,"client","pool","client");
        when(cognito.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(AdminGetUserResponse.builder().username("canonical-admin-id").build());
        when(decoder.decode("id-token")).thenReturn(Jwt.withTokenValue("id-token").header("alg","RS256").subject("admin").claim("token_use","id").audience(List.of("client")).expiresAt(java.time.Instant.now().plusSeconds(3600)).claim("cognito:groups",List.of("news-admin")).build());
        controller=new NewsMfaAuthController(cognito,auth,provider,sessions,new WebAuthCookieService(auth),pending);
    }
    AuthenticationResultType tokens(){return AuthenticationResultType.builder().idToken("id-token").accessToken("access-token").expiresIn(3600).build();}
    NewsMfaAuthController.Login login(){return new NewsMfaAuthController.Login("admin@example.org","test-password");}
    @Test void passwordOnlyResultRequiresVerifiedAuthenticatorBeforeCreatingSession(){
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder().authenticationResult(tokens()).build());
        when(cognito.associateSoftwareToken(any(AssociateSoftwareTokenRequest.class))).thenReturn(AssociateSoftwareTokenResponse.builder().secretCode("test-secret").build());
        var result=controller.login(login(),"1").getBody();
        assertThat(result.state()).isEqualTo("ENROLL");assertThat(result.secret()).isEqualTo("test-secret");verifyNoInteractions(sessions);
        when(cognito.verifySoftwareToken(any(VerifySoftwareTokenRequest.class))).thenReturn(VerifySoftwareTokenResponse.builder().status(VerifySoftwareTokenResponseType.SUCCESS).build());
        var response=new MockHttpServletResponse();
        assertThat(controller.verify(new NewsMfaAuthController.Code(result.challengeId(),"123456"),"1",response).getBody().state()).isEqualTo("AUTHENTICATED");
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2);verify(sessions).issue(argThat(jwt->"id-token".equals(jwt.getTokenValue())),intThat(seconds->seconds>0&&seconds<=3600));
    }
    @Test void successfulMfaIssuesBoundSessionAndRejectsReplay(){
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder().challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA).session("cognito-session").build());
        when(cognito.respondToAuthChallenge(any(RespondToAuthChallengeRequest.class))).thenReturn(RespondToAuthChallengeResponse.builder().authenticationResult(tokens()).build());
        when(sessions.issue(any(Jwt.class),eq(3600))).thenReturn("opaque-proof");
        var result=controller.login(login(),"1").getBody();var code=new NewsMfaAuthController.Code(result.challengeId(),"123456");
        var response=new MockHttpServletResponse();assertThat(controller.verify(code,"1",response).getBody().state()).isEqualTo("AUTHENTICATED");
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2).allMatch(c->c.contains("HttpOnly")&&c.contains("Path=/admin"));
        assertThatThrownBy(()->controller.verify(code,"1",new MockHttpServletResponse())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("401");
        verify(sessions,times(1)).issue(any(Jwt.class),eq(3600));
        verify(cognito).respondToAuthChallenge(argThat((RespondToAuthChallengeRequest request)->"canonical-admin-id".equals(request.challengeResponses().get("USERNAME"))));
    }
    @Test void wrongCodesAreLimitedToFiveAttempts(){
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder().challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA).session("session").build());
        when(cognito.respondToAuthChallenge(any(RespondToAuthChallengeRequest.class))).thenThrow(CodeMismatchException.builder().message("wrong code").build());
        var result=controller.login(login(),"1").getBody();var code=new NewsMfaAuthController.Code(result.challengeId(),"000000");
        for(int i=0;i<6;i++)assertThatThrownBy(()->controller.verify(code,"1",new MockHttpServletResponse())).isInstanceOf(ResponseStatusException.class);
        verify(cognito,times(5)).respondToAuthChallenge(any(RespondToAuthChallengeRequest.class));verifyNoInteractions(sessions);
    }
    @Test void disabledAuthAndMissingHeaderCannotStartMfa(){
        assertThatThrownBy(()->controller.login(login(),"")).isInstanceOf(ResponseStatusException.class);
        auth.setEnabled(false);assertThatThrownBy(()->controller.login(login(),"1")).isInstanceOf(ResponseStatusException.class);verifyNoInteractions(cognito,sessions);
    }
    @Test void nonAdminCannotEnrollThroughAdminEndpoint(){
        when(cognito.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(InitiateAuthResponse.builder().authenticationResult(tokens()).build());
        when(decoder.decode("id-token")).thenReturn(Jwt.withTokenValue("id-token").header("alg","RS256").subject("reader").build());
        assertThatThrownBy(()->controller.login(login(),"1")).isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
        verify(cognito,never()).associateSoftwareToken(any(AssociateSoftwareTokenRequest.class));verifyNoInteractions(sessions);
    }
}
