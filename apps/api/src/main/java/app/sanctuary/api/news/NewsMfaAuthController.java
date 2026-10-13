package app.sanctuary.api.news;

import java.time.Instant;
import java.util.*;
import app.sanctuary.api.news.NewsAuthStateStore.Pending;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import app.sanctuary.api.auth.service.WebAuthCookieService;
import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

@RestController
public class NewsMfaAuthController {
    private final CognitoIdentityProviderClient cognito;
    private final AuthProperties auth;
    private final ObjectProvider<JwtDecoder> decoder;
    private final NewsMfaSessions sessions;
    private final WebAuthCookieService cookies;
    private final NewsAuthStateStore pending;
    public record Login(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=256) String password){@Override public String toString(){return "Login[redacted]";}}
    public record Code(@NotBlank @Size(max=100) String challengeId,@Pattern(regexp="[0-9]{6}") @NotNull String code){@Override public String toString(){return "Code[redacted]";}}
    public record Result(String state,String challengeId,String secret,String message){}
    public NewsMfaAuthController(CognitoIdentityProviderClient cognito,AuthProperties auth,ObjectProvider<JwtDecoder> decoder,NewsMfaSessions sessions,WebAuthCookieService cookies,NewsAuthStateStore pending){this.cognito=cognito;this.auth=auth;this.decoder=decoder;this.sessions=sessions;this.cookies=cookies;this.pending=pending;}
    @PostMapping("/auth/news/login")
    public ResponseEntity<Result> login(@Valid @RequestBody Login input,@RequestHeader(value="X-Sanctuary-News-Admin",defaultValue="") String header){
        check(header);if(!pending.allowLogin(input.email()))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many attempts. Wait before signing in again.");
        try{
            var response=cognito.initiateAuth(InitiateAuthRequest.builder().clientId(auth.clientId()).authFlow(AuthFlowType.USER_PASSWORD_AUTH).authParameters(Map.of("USERNAME",input.email().trim().toLowerCase(Locale.ROOT),"PASSWORD",input.password())).build());
            if(response.challengeName()==ChallengeNameType.SOFTWARE_TOKEN_MFA){
                String username=response.challengeParameters().get("USER_ID_FOR_SRP");
                if(username==null||username.isBlank()) username=response.challengeParameters().get("USERNAME");
                // Cognito challenge responses require the canonical username, not an email alias.
                if(username==null||username.isBlank()||username.contains("@")) {
                    username=cognito.adminGetUser(AdminGetUserRequest.builder().userPoolId(auth.userPoolId()).username(input.email().trim().toLowerCase(Locale.ROOT)).build()).username();
                }
                if(username==null||username.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Could not identify this sign-in session. Start sign-in again.");
                String id=UUID.randomUUID().toString();pending.put(id,new Pending(username,response.session(),null,null,Instant.now().plusSeconds(300),0));
                return result(new Result("CODE",id,null,"Enter your authenticator code."));
            }
            var tokens=response.authenticationResult();
            if(tokens==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete account confirmation/password setup before administrator sign-in.");
            requireAdmin(tokens.idToken());
            // A password-only response never creates an admin session. Verify authenticator possession first.
            var association=cognito.associateSoftwareToken(AssociateSoftwareTokenRequest.builder().accessToken(tokens.accessToken()).build());
            String id=UUID.randomUUID().toString();pending.put(id,new Pending(input.email(),null,tokens.accessToken(),tokens.idToken(),Instant.now().plusSeconds(300),0));
            return result(new Result("ENROLL",id,association.secretCode(),"Add this secret to your authenticator, then enter its six-digit code."));
        }catch(CognitoIdentityProviderException e){throw failure(e);}
    }
    @PostMapping("/auth/news/verify")
    public ResponseEntity<Result> verify(@Valid @RequestBody Code input,@RequestHeader(value="X-Sanctuary-News-Admin",defaultValue="") String header,HttpServletResponse response){
        check(header);Pending state=pending.take(input.challengeId());
        if(state==null||!state.expires().isAfter(Instant.now())||state.attempts()>=5)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Challenge expired. Sign in again.");
        try{
            if(state.accessToken()!=null){
                var verified=cognito.verifySoftwareToken(VerifySoftwareTokenRequest.builder().accessToken(state.accessToken()).userCode(input.code()).friendlyDeviceName("Sanctuary administrator").build());
                if(verified.status()!=VerifySoftwareTokenResponseType.SUCCESS)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid authenticator code. Sign in and start setup again.");
                cognito.setUserMFAPreference(SetUserMfaPreferenceRequest.builder().accessToken(state.accessToken()).softwareTokenMfaSettings(SoftwareTokenMfaSettingsType.builder().enabled(true).preferredMfa(true).build()).build());
                Jwt identity=requireAdmin(state.idToken());
                if(identity.getExpiresAt()==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in again to finish setup.");
                int remaining=(int)Math.min(3600,java.time.Duration.between(Instant.now(),identity.getExpiresAt()).getSeconds());
                if(remaining<=0)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in again to finish setup.");
                String proof=sessions.issue(identity,remaining);
                cookies.setNewsSessionCookies(response,state.idToken(),proof,remaining);
                return result(new Result("AUTHENTICATED",null,null,"Authenticator verified. You’re signed in."));
            }
            var verified=cognito.respondToAuthChallenge(RespondToAuthChallengeRequest.builder().clientId(auth.clientId()).challengeName(ChallengeNameType.SOFTWARE_TOKEN_MFA).session(state.session()).challengeResponses(Map.of("USERNAME",state.username(),"SOFTWARE_TOKEN_MFA_CODE",input.code())).build());
            var tokens=verified.authenticationResult();
            if(tokens==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"MFA did not complete. Sign in again.");
            Jwt identity=requireAdmin(tokens.idToken());
            int duration=Math.min(tokens.expiresIn(),3600);
            String proof=sessions.issue(identity,duration);
            cookies.setNewsSessionCookies(response,tokens.idToken(),proof,duration);
            return result(new Result("AUTHENTICATED",null,null,"Administrator signed in with MFA."));
        }catch(CodeMismatchException e){if(state.attempts()<4)pending.put(input.challengeId(),new Pending(state.username(),state.session(),state.accessToken(),state.idToken(),state.expires(),state.attempts()+1));throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Incorrect authenticator code. Try again.");}
        catch(CognitoIdentityProviderException e){throw failure(e);}
    }
    @PostMapping("/admin/news/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value="X-Sanctuary-News-Admin",defaultValue="") String header,HttpServletRequest request,HttpServletResponse response){
        check(header);sessions.revoke(request);cookies.clearNewsSessionCookies(response);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    private Jwt requireAdmin(String token){
        JwtDecoder verifier=decoder.getIfAvailable();if(verifier==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        Jwt jwt;
        try{jwt=verifier.decode(token);}catch(JwtException exception){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Your sign-in session expired. Start sign-in again.");}
        if(!NewsIdentityPolicy.accepts(jwt,auth))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"This account is not a news administrator.");
        return jwt;
    }
    private void check(String header){if(!auth.enabled()||!"1".equals(header))throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
    private ResponseEntity<Result> result(Result result){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(result);}
    private ResponseStatusException failure(CognitoIdentityProviderException e){
        org.slf4j.LoggerFactory.getLogger(NewsMfaAuthController.class).warn("Administrator Cognito authentication failed: {}",e.awsErrorDetails()==null?e.getClass().getSimpleName():e.awsErrorDetails().errorCode());
        String message=e instanceof ExpiredCodeException?"That code or sign-in session expired. Start sign-in again and use a fresh authenticator code.":e instanceof TooManyRequestsException?"Too many attempts. Wait a few minutes before signing in again.":"Sign-in could not be completed. Start sign-in again and enter a fresh authenticator code.";
        return new ResponseStatusException(e instanceof TooManyRequestsException?HttpStatus.TOO_MANY_REQUESTS:HttpStatus.UNAUTHORIZED,message);
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> error(ResponseStatusException error){
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore()).body(Map.of("message",error.getReason()==null?"Administrator authentication failed.":error.getReason()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> invalidInput(){
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message","Enter your email and password, or the six-digit authenticator code when prompted."));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String,String>> malformedJson(){
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message","Invalid request. Check the form and try again."));
    }

}
