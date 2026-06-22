package app.sanctuary.api.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.admin.dto.AdminUserAccessDto;
import app.sanctuary.api.admin.repository.AdminAuditRepository;
import app.sanctuary.api.admin.repository.AdminUserRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserRepository repository;

    @Mock
    private AdminAuditRepository auditRepository;

    @InjectMocks
    private AdminUserService service;

    @Test
    void searchRejectsShortEmailQuery() {
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.searchAdminAccess("ab", 10)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void searchClampsResultLimit() {
        List<AdminUserAccessDto> users = List.of(user(false));
        when(repository.searchAdminAccessByEmail("admin@example.com", 25)).thenReturn(users);

        List<AdminUserAccessDto> result = service.searchAdminAccess(" admin@example.com ", 100);

        assertEquals(users, result);
    }

    @Test
    void enablingAdminWritesAuditEvent() {
        UUID actorUserId = UUID.randomUUID();
        AdminUserAccessDto target = user(false);
        AdminUserAccessDto updated = new AdminUserAccessDto(
            target.userId(),
            target.email(),
            target.displayName(),
            true,
            target.registrationDate(),
            target.lastSignInAt()
        );
        when(repository.findAdminAccessByUserId(target.userId())).thenReturn(Optional.of(target), Optional.of(updated));
        when(repository.setAdminAccess(target.userId(), true)).thenReturn(updated);

        AdminUserAccessDto result = service.setAdminAccess(actorUserId, target.userId(), true);

        assertEquals(updated, result);
        verify(auditRepository).record(actorUserId, "admin.user.enable", "user", target.userId().toString());
    }

    @Test
    void disablingAnotherAdminWritesAuditEvent() {
        UUID actorUserId = UUID.randomUUID();
        AdminUserAccessDto target = user(true);
        AdminUserAccessDto updated = new AdminUserAccessDto(
            target.userId(),
            target.email(),
            target.displayName(),
            false,
            target.registrationDate(),
            target.lastSignInAt()
        );
        when(repository.findAdminAccessByUserId(target.userId())).thenReturn(Optional.of(target), Optional.of(updated));
        when(repository.setAdminAccess(target.userId(), false)).thenReturn(updated);

        AdminUserAccessDto result = service.setAdminAccess(actorUserId, target.userId(), false);

        assertEquals(updated, result);
        verify(auditRepository).record(actorUserId, "admin.user.disable", "user", target.userId().toString());
    }

    @Test
    void disablingSelfIsBlocked() {
        AdminUserAccessDto target = user(true);
        when(repository.findAdminAccessByUserId(target.userId())).thenReturn(Optional.of(target));

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.setAdminAccess(target.userId(), target.userId(), false)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verifyNoInteractions(auditRepository);
    }

    @Test
    void missingTargetReturnsNotFound() {
        UUID targetUserId = UUID.randomUUID();
        when(repository.findAdminAccessByUserId(targetUserId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.setAdminAccess(UUID.randomUUID(), targetUserId, true)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(auditRepository);
    }

    private AdminUserAccessDto user(boolean admin) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdminUserAccessDto(
            UUID.randomUUID(),
            "admin@example.com",
            "Admin User",
            admin,
            now.minusDays(7),
            now.minusHours(1)
        );
    }
}
