package app.sanctuary.api.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.auth.service.CognitoAuthService;
import app.sanctuary.api.config.AuthProperties;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private AuthProperties authProperties;

    @Mock
    private CognitoAuthService cognitoAuthService;

    @InjectMocks
    private AdminAuthorizationService service;

    @Test
    void requireAdminReturnsAccountWhenUserHasAdminGroup() {
        CurrentUser currentUser = currentUser(List.of("SanctuaryAdmins"));
        UserAccountDto account = account();
        when(userAccountService.ensureAccount(currentUser)).thenReturn(account);
        when(authProperties.adminGroup()).thenReturn("SanctuaryAdmins");

        UserAccountDto result = service.requireAdmin(currentUser);

        assertEquals(account, result);
        verify(cognitoAuthService, never()).isUserInGroup(currentUser.cognitoSub(), currentUser.email(), "SanctuaryAdmins");
    }

    @Test
    void requireAdminReturnsAccountWhenCognitoConfirmsAdminGroup() {
        CurrentUser currentUser = currentUser(List.of());
        UserAccountDto account = account();
        when(userAccountService.ensureAccount(currentUser)).thenReturn(account);
        when(authProperties.adminGroup()).thenReturn("SanctuaryAdmins");
        when(cognitoAuthService.isUserInGroup(currentUser.cognitoSub(), currentUser.email(), "SanctuaryAdmins")).thenReturn(true);

        UserAccountDto result = service.requireAdmin(currentUser);

        assertEquals(account, result);
    }

    @Test
    void requireAdminRejectsUserMissingAdminGroup() {
        CurrentUser currentUser = currentUser(List.of("OtherGroup"));
        UserAccountDto account = account();
        when(userAccountService.ensureAccount(currentUser)).thenReturn(account);
        when(authProperties.adminGroup()).thenReturn("SanctuaryAdmins");
        when(cognitoAuthService.isUserInGroup(currentUser.cognitoSub(), currentUser.email(), "SanctuaryAdmins")).thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.requireAdmin(currentUser)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    private CurrentUser currentUser(List<String> groups) {
        return new CurrentUser("cognito-sub-123", "admin@example.com", "Admin", "User", "Admin User", null, groups);
    }

    private UserAccountDto account() {
        return new UserAccountDto(
            UUID.randomUUID(),
            "cognito-sub-123",
            "admin@example.com",
            "Admin",
            "User",
            "Admin User",
            "en",
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );
    }
}
