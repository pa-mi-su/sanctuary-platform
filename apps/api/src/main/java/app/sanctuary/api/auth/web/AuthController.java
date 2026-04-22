package app.sanctuary.api.auth.web;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.auth.dto.AuthConfirmRegistrationRequest;
import app.sanctuary.api.auth.dto.AuthLoginRequest;
import app.sanctuary.api.auth.dto.AuthRegisterRequest;
import app.sanctuary.api.auth.dto.AuthRegistrationResponse;
import app.sanctuary.api.auth.dto.AuthResendCodeRequest;
import app.sanctuary.api.auth.dto.AuthSessionResponse;
import app.sanctuary.api.auth.dto.AuthStatusResponse;
import app.sanctuary.api.auth.service.AuthFlowException;
import app.sanctuary.api.auth.service.CognitoAuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CognitoAuthService cognitoAuthService;

    public AuthController(CognitoAuthService cognitoAuthService) {
        this.cognitoAuthService = cognitoAuthService;
    }

    @PostMapping("/register")
    public AuthRegistrationResponse register(@Valid @RequestBody AuthRegisterRequest request) {
        return cognitoAuthService.register(request);
    }

    @PostMapping("/confirm")
    public AuthStatusResponse confirm(@Valid @RequestBody AuthConfirmRegistrationRequest request) {
        return cognitoAuthService.confirm(request);
    }

    @PostMapping("/resend-confirmation")
    public AuthStatusResponse resendConfirmation(@Valid @RequestBody AuthResendCodeRequest request) {
        return cognitoAuthService.resendConfirmationCode(request.email());
    }

    @PostMapping("/login")
    public AuthSessionResponse login(@Valid @RequestBody AuthLoginRequest request) {
        return cognitoAuthService.login(request);
    }

    @ExceptionHandler(AuthFlowException.class)
    public ResponseEntity<Map<String, String>> handleAuthFlowException(AuthFlowException exception) {
        return ResponseEntity.status(exception.status())
            .body(Map.of("message", exception.getMessage()));
    }
}
