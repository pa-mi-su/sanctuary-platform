package app.sanctuary.api.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;
import app.sanctuary.api.admin.repository.AdminAuditRepository;
import app.sanctuary.api.admin.repository.AdminNotificationRepository;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private AdminNotificationRepository notificationRepository;

    @Mock
    private AdminAuditRepository auditRepository;

    @InjectMocks
    private AdminNotificationService service;

    @Test
    void createDraftPersistsNotificationAndAuditEvent() {
        UUID adminUserId = UUID.randomUUID();
        AdminNotificationRequest request = new AdminNotificationRequest("Update", "Please update Sanctuary.");
        AdminNotificationDto notification = notification();
        when(notificationRepository.createDraft(adminUserId, request)).thenReturn(notification);

        AdminNotificationDto result = service.createDraft(adminUserId, request);

        assertEquals(notification, result);
        verify(auditRepository).record(
            adminUserId,
            "admin.notification.create_draft",
            "admin_notification",
            notification.id().toString()
        );
    }

    @Test
    void historyClampsInvalidLimitToDefault() {
        List<AdminNotificationDto> history = List.of(notification());
        when(notificationRepository.history(50)).thenReturn(history);

        List<AdminNotificationDto> result = service.history(0);

        assertEquals(history, result);
    }

    @Test
    void historyClampsLargeLimitToMax() {
        List<AdminNotificationDto> history = List.of(notification());
        when(notificationRepository.history(250)).thenReturn(history);

        List<AdminNotificationDto> result = service.history(500);

        assertEquals(history, result);
    }

    private AdminNotificationDto notification() {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdminNotificationDto(
            UUID.randomUUID(),
            "Update",
            "Please update Sanctuary.",
            "all",
            "draft",
            0,
            0,
            0,
            null,
            now,
            now
        );
    }
}
