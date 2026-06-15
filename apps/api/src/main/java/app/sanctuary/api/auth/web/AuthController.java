package app.sanctuary.api.auth.web;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.auth.dto.AuthConfirmRegistrationRequest;
import app.sanctuary.api.auth.dto.AuthForgotPasswordRequest;
import app.sanctuary.api.auth.dto.AuthLoginRequest;
import app.sanctuary.api.auth.dto.AuthRefreshRequest;
import app.sanctuary.api.auth.dto.AuthRegisterRequest;
import app.sanctuary.api.auth.dto.AuthRegistrationResponse;
import app.sanctuary.api.auth.dto.AuthResetPasswordRequest;
import app.sanctuary.api.auth.dto.AuthResendCodeRequest;
import app.sanctuary.api.auth.dto.AuthSessionResponse;
import app.sanctuary.api.auth.dto.AuthStatusResponse;
import app.sanctuary.api.auth.dto.AuthWebSessionResponse;
import app.sanctuary.api.auth.service.AuthFlowException;
import app.sanctuary.api.auth.service.BlockedEmailDomainException;
import app.sanctuary.api.auth.service.CognitoAuthService;
import app.sanctuary.api.auth.service.WebAuthCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CognitoAuthService cognitoAuthService;
    private final WebAuthCookieService webAuthCookieService;

    public AuthController(CognitoAuthService cognitoAuthService, WebAuthCookieService webAuthCookieService) {
        this.cognitoAuthService = cognitoAuthService;
        this.webAuthCookieService = webAuthCookieService;
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

    @PostMapping("/refresh")
    public AuthSessionResponse refresh(@Valid @RequestBody AuthRefreshRequest request) {
        return cognitoAuthService.refresh(request);
    }

    @PostMapping("/web/login")
    public AuthWebSessionResponse webLogin(
        @Valid @RequestBody AuthLoginRequest request,
        HttpServletResponse response
    ) {
        AuthSessionResponse session = cognitoAuthService.login(request);
        webAuthCookieService.setSessionCookies(response, session);
        return AuthWebSessionResponse.from(session);
    }

    @PostMapping("/web/refresh")
    public AuthWebSessionResponse webRefresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = webAuthCookieService.refreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthFlowException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Your session has ended. Please sign in again.");
        }

        AuthSessionResponse session = cognitoAuthService.refresh(new AuthRefreshRequest(refreshToken));
        webAuthCookieService.setSessionCookies(response, session);
        return AuthWebSessionResponse.from(session);
    }

    @PostMapping("/web/logout")
    public ResponseEntity<Void> webLogout(HttpServletResponse response) {
        webAuthCookieService.clearSessionCookies(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    public AuthStatusResponse forgotPassword(@Valid @RequestBody AuthForgotPasswordRequest request) {
        return cognitoAuthService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public AuthSessionResponse resetPassword(@Valid @RequestBody AuthResetPasswordRequest request) {
        return cognitoAuthService.resetPassword(request);
    }

    @PostMapping("/web/reset-password")
    public AuthWebSessionResponse webResetPassword(
        @Valid @RequestBody AuthResetPasswordRequest request,
        HttpServletResponse response
    ) {
        AuthSessionResponse session = cognitoAuthService.resetPassword(request);
        webAuthCookieService.setSessionCookies(response, session);
        return AuthWebSessionResponse.from(session);
    }

    @ExceptionHandler(AuthFlowException.class)
    public ResponseEntity<Map<String, String>> handleAuthFlowException(AuthFlowException exception) {
        return ResponseEntity.status(exception.status())
            .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(BlockedEmailDomainException.class)
    public ResponseEntity<Map<String, String>> handleBlockedEmailDomainException(BlockedEmailDomainException exception) {
        return ResponseEntity.badRequest()
            .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map((error) -> error.getDefaultMessage())
            .filter((value) -> value != null && !value.isBlank())
            .findFirst()
            .orElse("Please check the highlighted fields and try again.");

        String details = exception.getBindingResult().getFieldErrors().stream()
            .map((error) -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(
            details.isBlank()
                ? Map.of("message", message)
                : Map.of("message", message, "details", details)
        );
    }
}
