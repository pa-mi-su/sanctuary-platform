package app.sanctuary.api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.sanctuary.api.auth.service.AuthFlowException;
import app.sanctuary.api.auth.service.CognitoAuthService;
import app.sanctuary.api.user.dto.UserAccountDto;
import app.sanctuary.api.user.repository.UserAccountRepository;
import app.sanctuary.api.user.web.CurrentUser;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock
    private UserAccountRepository repository;

    @Mock
    private CognitoAuthService cognitoAuthService;

    @InjectMocks
    private UserAccountService service;

    @Test
    void ensureAccountUpsertsCurrentUser() {
        CurrentUser currentUser = new CurrentUser("cognito-sub-123", "saint@example.com", "Saint", "User", "Saint User", "https://example.com/avatar.png");
        UserAccountDto account = new UserAccountDto(
            UUID.randomUUID(),
            currentUser.cognitoSub(),
            currentUser.email(),
            currentUser.firstName(),
            currentUser.lastName(),
            currentUser.displayName(),
            "en",
            currentUser.avatarUrl(),
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );

        when(repository.upsert(
            eq(currentUser.cognitoSub()),
            eq(currentUser.email()),
            eq(currentUser.firstName()),
            eq(currentUser.lastName()),
            eq(currentUser.displayName()),
            eq(currentUser.avatarUrl())
        ))
            .thenReturn(account);
        when(repository.isDeletedIdentity(eq(currentUser.cognitoSub()), isNull()))
            .thenReturn(false);

        UserAccountDto result = service.ensureAccount(currentUser);

        assertEquals(account, result);
        verify(repository).upsert(
            currentUser.cognitoSub(),
            currentUser.email(),
            currentUser.firstName(),
            currentUser.lastName(),
            currentUser.displayName(),
            currentUser.avatarUrl()
        );
    }

    @Test
    void ensureAccountRejectsMissingCognitoSubject() {
        CurrentUser currentUser = new CurrentUser(" ", "saint@example.com", "Saint", "User", "Saint User", null);

        assertThrows(IllegalArgumentException.class, () -> service.ensureAccount(currentUser));
    }

    @Test
    void ensureAccountRejectsDeletedIdentity() {
        CurrentUser currentUser = new CurrentUser("cognito-sub-123", "saint@example.com", "Saint", "User", "Saint User", null);
        when(repository.isDeletedIdentity(eq(currentUser.cognitoSub()), isNull()))
            .thenReturn(true);

        assertThrows(AuthFlowException.class, () -> service.ensureAccount(currentUser));
    }

    @Test
    void deleteAccountDeletesCognitoUserThenLocalAccount() {
        CurrentUser currentUser = new CurrentUser("cognito-sub-123", "saint@example.com", "Saint", "User", "Saint User", null);
        UserAccountDto account = new UserAccountDto(
            UUID.randomUUID(),
            currentUser.cognitoSub(),
            currentUser.email(),
            currentUser.firstName(),
            currentUser.lastName(),
            currentUser.displayName(),
            "en",
            null,
            OffsetDateTime.now(),
            OffsetDateTime.now()
        );

        when(repository.upsert(
            eq(currentUser.cognitoSub()),
            eq(currentUser.email()),
            eq(currentUser.firstName()),
            eq(currentUser.lastName()),
            eq(currentUser.displayName()),
            eq(currentUser.avatarUrl())
        ))
            .thenReturn(account);
        when(repository.isDeletedIdentity(eq(currentUser.cognitoSub()), isNull()))
            .thenReturn(false);

        service.deleteAccount(currentUser);

        verify(cognitoAuthService).deleteUser(currentUser.cognitoSub(), currentUser.email());
        verify(repository).markDeleted(currentUser.cognitoSub(), "8b268da13a7dc4b60332e56a7fd28ec6002733a56eb680c889e1eb5d66066d6c");
        verify(repository).deleteById(account.id());
    }
}
