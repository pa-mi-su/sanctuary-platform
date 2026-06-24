package app.sanctuary.api.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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

import app.sanctuary.api.admin.config.AdminProperties;
import app.sanctuary.api.admin.repository.AdminAuthorizationRepository;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.service.UserAccountService;
import app.sanctuary.api.user.web.CurrentUser;

@ExtendWith(MockitoExtension.class)
class AdminAuthorizationServiceTest {

    @Mock
    private UserAccountService userAccountService;

    @Mock
    private AdminAuthorizationRepository adminAuthorizationRepository;

    @Mock
    private AdminProperties adminProperties;

    @InjectMocks
    private AdminAuthorizationService service;

    @Test
    void requireAdminReturnsAccountWhenUserIsEnabledAdmin() {
        CurrentUser currentUser = currentUser();
        UserAccountDto account = account();
        when(userAccountService.ensureAccount(currentUser)).thenReturn(account);
        when(adminAuthorizationRepository.isAdmin(account.id())).thenReturn(true);

        UserAccountDto result = service.requireAdmin(currentUser);

        assertEquals(account, result);
        verify(adminAuthorizationRepository).isAdmin(account.id());
        verify(adminAuthorizationRepository, never()).grantAdmin(account.id(), "Bootstrapped from configured admin email.");
    }

    @Test
    void requireAdminRejectsNonAdminUser() {
        CurrentUser currentUser = currentUser();
        UserAccountDto account = account();
        when(userAccountService.ensureAccount(currentUser)).thenReturn(account);
        when(adminAuthorizationRepository.isAdmin(account.id())).thenReturn(false);
        when(adminProperties.isBootstrapAdmin(account.email())).thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.requireAdmin(currentUser)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void requireAdminBootstrapsConfiguredAdminEmail() {
        CurrentUser currentUser = currentUser();
        UserAccountDto account = account();
        when(userAccountService.ensureAccount(currentUser)).thenReturn(account);
        when(adminAuthorizationRepository.isAdmin(account.id())).thenReturn(false);
        when(adminProperties.isBootstrapAdmin(account.email())).thenReturn(true);

        UserAccountDto result = service.requireAdmin(currentUser);

        assertEquals(account, result);
        verify(adminAuthorizationRepository).grantAdmin(account.id(), "Bootstrapped from configured admin email.");
    }

    @Test
    void adminPropertiesMatchBootstrapEmailCaseInsensitively() {
        AdminProperties properties = new AdminProperties(List.of("Admin@Example.com"));

        assertEquals(true, properties.isBootstrapAdmin(" admin@example.com "));
    }

    private CurrentUser currentUser() {
        return new CurrentUser("cognito-sub-123", "admin@example.com", "Admin", "User", "Admin User", null);
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
