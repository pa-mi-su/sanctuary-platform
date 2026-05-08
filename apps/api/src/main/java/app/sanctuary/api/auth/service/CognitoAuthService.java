package app.sanctuary.api.auth.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import app.sanctuary.api.auth.dto.AuthConfirmRegistrationRequest;
import app.sanctuary.api.auth.dto.AuthForgotPasswordRequest;
import app.sanctuary.api.auth.dto.AuthLoginRequest;
import app.sanctuary.api.auth.dto.AuthRefreshRequest;
import app.sanctuary.api.auth.dto.AuthRegisterRequest;
import app.sanctuary.api.auth.dto.AuthRegistrationResponse;
import app.sanctuary.api.auth.dto.AuthResetPasswordRequest;
import app.sanctuary.api.auth.dto.AuthSessionResponse;
import app.sanctuary.api.auth.dto.AuthStatusResponse;
import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeDeliveryFailureException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ForgotPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.TooManyRequestsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

@Service
public class CognitoAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final AuthProperties authProperties;

    public CognitoAuthService(CognitoIdentityProviderClient cognitoClient, AuthProperties authProperties) {
        this.cognitoClient = cognitoClient;
        this.authProperties = authProperties;
    }

    public AuthRegistrationResponse register(AuthRegisterRequest request) {
        validateConfigured();

        String email = normalizedEmail(request.email());
        String firstName = cleaned(request.firstName());
        String lastName = cleaned(request.lastName());

        try {
            cognitoClient.signUp(SignUpRequest.builder()
                .clientId(authProperties.clientId())
                .username(email)
                .password(request.password())
                .userAttributes(List.of(
                    attribute("email", email),
                    attribute("given_name", firstName),
                    attribute("family_name", lastName),
                    attribute("name", displayName(firstName, lastName))
                ))
                .build());

            return new AuthRegistrationResponse(email, displayName(firstName, lastName), true);
        } catch (UsernameExistsException exception) {
            throw new AuthFlowException(HttpStatus.CONFLICT, "An account with this email already exists.");
        } catch (InvalidPasswordException exception) {
            throw new AuthFlowException(
                HttpStatus.BAD_REQUEST,
                "Choose a password with at least 8 characters, plus uppercase, lowercase, number, and special character."
            );
        } catch (TooManyRequestsException exception) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait a moment and try again.");
        } catch (CodeDeliveryFailureException exception) {
            throw new AuthFlowException(HttpStatus.BAD_GATEWAY, "We could not send a confirmation email right now.");
        } catch (InvalidParameterException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, friendlyMessage(exception.getMessage(), "We could not create this account."));
        }
    }

    public AuthStatusResponse confirm(AuthConfirmRegistrationRequest request) {
        validateConfigured();

        try {
            cognitoClient.confirmSignUp(ConfirmSignUpRequest.builder()
                .clientId(authProperties.clientId())
                .username(normalizedEmail(request.email()))
                .confirmationCode(cleaned(request.code()))
                .build());
            return new AuthStatusResponse("Your account is confirmed. You can sign in now.");
        } catch (CodeMismatchException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "That confirmation code does not match. Please try again.");
        } catch (ExpiredCodeException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "That confirmation code has expired. Please request a new one.");
        } catch (UserNotFoundException exception) {
            throw new AuthFlowException(HttpStatus.NOT_FOUND, "We could not find an account for that email.");
        } catch (InvalidParameterException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, friendlyMessage(exception.getMessage(), "We could not confirm this account."));
        }
    }

    public AuthStatusResponse resendConfirmationCode(String email) {
        validateConfigured();

        try {
            cognitoClient.resendConfirmationCode(ResendConfirmationCodeRequest.builder()
                .clientId(authProperties.clientId())
                .username(normalizedEmail(email))
                .build());
            return new AuthStatusResponse("A new confirmation code is on the way.");
        } catch (UserNotFoundException exception) {
            throw new AuthFlowException(HttpStatus.NOT_FOUND, "We could not find an account for that email.");
        } catch (TooManyRequestsException exception) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait a moment and try again.");
        }
    }

    public AuthStatusResponse forgotPassword(AuthForgotPasswordRequest request) {
        validateConfigured();

        try {
            cognitoClient.forgotPassword(ForgotPasswordRequest.builder()
                .clientId(authProperties.clientId())
                .username(normalizedEmail(request.email()))
                .build());
        } catch (UserNotConfirmedException exception) {
            throw new AuthFlowException(HttpStatus.CONFLICT, "Please confirm your account before resetting your password.");
        } catch (TooManyRequestsException exception) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait a moment and try again.");
        } catch (CodeDeliveryFailureException exception) {
            throw new AuthFlowException(HttpStatus.BAD_GATEWAY, "We could not send a password reset code right now.");
        } catch (InvalidParameterException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, friendlyMessage(exception.getMessage(), "We could not start password reset."));
        } catch (UserNotFoundException exception) {
            // Return the same professional response instead of exposing whether the account exists.
        }

        return new AuthStatusResponse("If that email belongs to a Sanctuary account, a password reset code is on the way.");
    }

    public AuthStatusResponse resetPassword(AuthResetPasswordRequest request) {
        validateConfigured();

        try {
            cognitoClient.confirmForgotPassword(ConfirmForgotPasswordRequest.builder()
                .clientId(authProperties.clientId())
                .username(normalizedEmail(request.email()))
                .confirmationCode(cleaned(request.code()))
                .password(request.newPassword())
                .build());
            return new AuthStatusResponse("Your password is updated. Please sign in to continue.");
        } catch (CodeMismatchException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "That reset code does not match. Please try again.");
        } catch (ExpiredCodeException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, "That reset code has expired. Please request a new one.");
        } catch (InvalidPasswordException exception) {
            throw new AuthFlowException(
                HttpStatus.BAD_REQUEST,
                "Choose a password with at least 8 characters, plus uppercase, lowercase, number, and special character."
            );
        } catch (TooManyRequestsException exception) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait a moment and try again.");
        } catch (UserNotFoundException exception) {
            throw new AuthFlowException(HttpStatus.NOT_FOUND, "We could not find an account for that email.");
        } catch (InvalidParameterException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, friendlyMessage(exception.getMessage(), "We could not reset this password."));
        }
    }

    public AuthSessionResponse login(AuthLoginRequest request) {
        validateConfigured();

        try {
            var response = cognitoClient.initiateAuth(InitiateAuthRequest.builder()
                .clientId(authProperties.clientId())
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(Map.of(
                    "USERNAME", normalizedEmail(request.email()),
                    "PASSWORD", request.password()
                ))
                .build());

            var authenticationResult = response.authenticationResult();
            if (authenticationResult == null || authenticationResult.accessToken() == null || authenticationResult.idToken() == null) {
                throw new AuthFlowException(HttpStatus.BAD_GATEWAY, "Sanctuary could not complete sign in.");
            }

            return new AuthSessionResponse(
                authenticationResult.accessToken(),
                authenticationResult.idToken(),
                authenticationResult.refreshToken(),
                authenticationResult.tokenType(),
                authenticationResult.expiresIn(),
                normalizedEmail(request.email()),
                extractDisplayName(authenticationResult.idToken(), normalizedEmail(request.email()))
            );
        } catch (UserNotConfirmedException exception) {
            throw new AuthFlowException(HttpStatus.CONFLICT, "Please confirm your account before signing in.");
        } catch (NotAuthorizedException exception) {
            throw new AuthFlowException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        } catch (UserNotFoundException exception) {
            throw new AuthFlowException(HttpStatus.NOT_FOUND, "We could not find an account for that email.");
        } catch (TooManyRequestsException exception) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait a moment and try again.");
        }
    }

    public AuthSessionResponse refresh(AuthRefreshRequest request) {
        validateConfigured();

        try {
            var response = cognitoClient.initiateAuth(InitiateAuthRequest.builder()
                .clientId(authProperties.clientId())
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .authParameters(Map.of("REFRESH_TOKEN", cleaned(request.refreshToken())))
                .build());

            var authenticationResult = response.authenticationResult();
            if (authenticationResult == null || authenticationResult.accessToken() == null || authenticationResult.idToken() == null) {
                throw new AuthFlowException(HttpStatus.BAD_GATEWAY, "Sanctuary could not refresh your session.");
            }

            String email = extractClaim(authenticationResult.idToken(), "\"email\":\"");
            String fallbackEmail = email == null || email.isBlank() ? "" : email;

            return new AuthSessionResponse(
                authenticationResult.accessToken(),
                authenticationResult.idToken(),
                request.refreshToken(),
                authenticationResult.tokenType(),
                authenticationResult.expiresIn(),
                fallbackEmail,
                extractDisplayName(authenticationResult.idToken(), fallbackEmail)
            );
        } catch (NotAuthorizedException exception) {
            throw new AuthFlowException(HttpStatus.UNAUTHORIZED, "Your session has ended. Please sign in again.");
        } catch (TooManyRequestsException exception) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait a moment and try again.");
        } catch (InvalidParameterException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, friendlyMessage(exception.getMessage(), "Sanctuary could not refresh your session."));
        }
    }

    public void deleteUser(String username) {
        validateConfigured();

        if (authProperties.userPoolId() == null || authProperties.userPoolId().isBlank()) {
            throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication is not configured for this environment yet.");
        }

        try {
            cognitoClient.adminDeleteUser(AdminDeleteUserRequest.builder()
                .userPoolId(authProperties.userPoolId())
                .username(cleaned(username))
                .build());
        } catch (UserNotFoundException exception) {
            // Treat this as already deleted so local Sanctuary data can still be removed.
        } catch (NotAuthorizedException exception) {
            throw new AuthFlowException(HttpStatus.FORBIDDEN, "Sanctuary could not delete this account right now.");
        } catch (TooManyRequestsException exception) {
            throw new AuthFlowException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait a moment and try again.");
        } catch (InvalidParameterException exception) {
            throw new AuthFlowException(HttpStatus.BAD_REQUEST, friendlyMessage(exception.getMessage(), "Sanctuary could not delete this account."));
        } catch (CognitoIdentityProviderException exception) {
            throw new AuthFlowException(HttpStatus.BAD_GATEWAY, "Sanctuary could not delete this account right now.");
        }
    }

    private void validateConfigured() {
        if (authProperties.clientId() == null || authProperties.clientId().isBlank()) {
            throw new AuthFlowException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication is not configured for this environment yet.");
        }
    }

    private AttributeType attribute(String name, String value) {
        return AttributeType.builder()
            .name(name)
            .value(value)
            .build();
    }

    private String cleaned(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizedEmail(String value) {
        return cleaned(value).toLowerCase();
    }

    private String displayName(String firstName, String lastName) {
        return (firstName + " " + lastName).trim();
    }

    private String extractDisplayName(String idToken, String fallback) {
        try {
            String json = decodeTokenPayload(idToken);
            String name = extractJsonString(json, "\"name\":\"");
            if (name != null && !name.isBlank()) {
                return name;
            }
            String givenName = extractJsonString(json, "\"given_name\":\"");
            String familyName = extractJsonString(json, "\"family_name\":\"");
            String joined = displayName(givenName == null ? "" : givenName, familyName == null ? "" : familyName);
            return joined.isBlank() ? fallback : joined;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private String extractClaim(String idToken, String marker) {
        try {
            return extractJsonString(decodeTokenPayload(idToken), marker);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String decodeTokenPayload(String idToken) {
        String payload = idToken.split("\\.")[1];
        String normalized = payload.replace('-', '+').replace('_', '/');
        while (normalized.length() % 4 != 0) {
            normalized += "=";
        }
        return new String(java.util.Base64.getDecoder().decode(normalized));
    }

    private String extractJsonString(String json, String marker) {
        int startIndex = json.indexOf(marker);
        if (startIndex < 0) {
            return null;
        }
        int valueStart = startIndex + marker.length();
        int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return null;
        }
        return json.substring(valueStart, valueEnd).replace("\\u0027", "'").replace("\\\"", "\"");
    }

    private String friendlyMessage(String rawMessage, String fallback) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return fallback;
        }

        String normalized = rawMessage.toLowerCase();
        if (normalized.contains("password")) {
            return "Choose a password with at least 8 characters, plus uppercase, lowercase, number, and special character.";
        }

        return rawMessage;
    }
}
