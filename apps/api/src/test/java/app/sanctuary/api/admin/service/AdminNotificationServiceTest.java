package app.sanctuary.api.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;
import app.sanctuary.api.admin.dto.AdminNotificationSendResultDto;
import app.sanctuary.api.admin.notification.PushNotificationGateway;
import app.sanctuary.api.admin.notification.PushNotificationPayload;
import app.sanctuary.api.admin.notification.PushNotificationSendResult;
import app.sanctuary.api.admin.notification.PushNotificationTarget;
import app.sanctuary.api.admin.repository.AdminAuditRepository;
import app.sanctuary.api.admin.repository.AdminNotificationRepository;

@ExtendWith(MockitoExtension.class)
class AdminNotificationServiceTest {

    @Mock
    private AdminNotificationRepository notificationRepository;

    @Mock
    private AdminAuditRepository auditRepository;

    @Mock
    private PushNotificationGateway pushNotificationGateway;

    @InjectMocks
    private AdminNotificationService service;

    @Test
    void sendCreatesNotificationAndAuditEvent() {
        UUID adminUserId = UUID.randomUUID();
        AdminNotificationRequest request = new AdminNotificationRequest("Update", "Please update Sanctuary.");
        AdminNotificationDto notification = notification();
        PushNotificationTarget target = target();
        UUID deliveryId = UUID.randomUUID();
        when(pushNotificationGateway.enabled()).thenReturn(true);
        when(notificationRepository.createDraft(adminUserId, request)).thenReturn(notification);
        when(notificationRepository.findValidTargetsForAllAudience()).thenReturn(List.of(target));
        when(notificationRepository.markSending(notification.id(), adminUserId, 1)).thenReturn(true);
        when(notificationRepository.createDelivery(notification.id(), target)).thenReturn(deliveryId);
        when(pushNotificationGateway.send(target, new PushNotificationPayload(notification.id(), notification.title(), notification.message())))
            .thenReturn(PushNotificationSendResult.sent("firebase-message-id"));

        AdminNotificationSendResultDto result = service.send(adminUserId, request);

        assertEquals("sent", result.status());
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

    @Test
    void sendRejectsWhenFirebaseGatewayIsDisabled() {
        when(pushNotificationGateway.enabled()).thenReturn(false);

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.send(UUID.randomUUID(), new AdminNotificationRequest("Update", "Please update Sanctuary."))
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void sendRejectsDraftWithNoValidDevices() {
        UUID adminUserId = UUID.randomUUID();
        AdminNotificationRequest request = new AdminNotificationRequest("Update", "Please update Sanctuary.");
        AdminNotificationDto notification = notification();
        when(pushNotificationGateway.enabled()).thenReturn(true);
        when(notificationRepository.createDraft(adminUserId, request)).thenReturn(notification);
        when(notificationRepository.findValidTargetsForAllAudience()).thenReturn(List.of());

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> service.send(adminUserId, request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void sendRecordsSuccessfulDeliveries() {
        UUID adminUserId = UUID.randomUUID();
        AdminNotificationRequest request = new AdminNotificationRequest("Update", "Please update Sanctuary.");
        AdminNotificationDto notification = notification();
        PushNotificationTarget target = target();
        UUID deliveryId = UUID.randomUUID();
        when(pushNotificationGateway.enabled()).thenReturn(true);
        when(notificationRepository.createDraft(adminUserId, request)).thenReturn(notification);
        when(notificationRepository.findValidTargetsForAllAudience()).thenReturn(List.of(target));
        when(notificationRepository.markSending(notification.id(), adminUserId, 1)).thenReturn(true);
        when(notificationRepository.createDelivery(notification.id(), target)).thenReturn(deliveryId);
        when(pushNotificationGateway.send(target, new PushNotificationPayload(notification.id(), notification.title(), notification.message())))
            .thenReturn(PushNotificationSendResult.sent("firebase-message-id"));

        AdminNotificationSendResultDto result = service.send(adminUserId, request);

        assertEquals("sent", result.status());
        assertEquals(1, result.targetCount());
        assertEquals(1, result.sentCount());
        assertEquals(0, result.failedCount());
        verify(notificationRepository).markDeliverySent(deliveryId);
        verify(notificationRepository).finishSend(notification.id(), "sent", 1, 0);
    }

    @Test
    void sendRecordsFailuresAndInvalidatesBadTokens() {
        UUID adminUserId = UUID.randomUUID();
        AdminNotificationRequest request = new AdminNotificationRequest("Update", "Please update Sanctuary.");
        AdminNotificationDto notification = notification();
        PushNotificationTarget target = target();
        UUID deliveryId = UUID.randomUUID();
        when(pushNotificationGateway.enabled()).thenReturn(true);
        when(notificationRepository.createDraft(adminUserId, request)).thenReturn(notification);
        when(notificationRepository.findValidTargetsForAllAudience()).thenReturn(List.of(target));
        when(notificationRepository.markSending(notification.id(), adminUserId, 1)).thenReturn(true);
        when(notificationRepository.createDelivery(notification.id(), target)).thenReturn(deliveryId);
        when(pushNotificationGateway.send(target, new PushNotificationPayload(notification.id(), notification.title(), notification.message())))
            .thenReturn(PushNotificationSendResult.failed("registration token is not valid", true));

        AdminNotificationSendResultDto result = service.send(adminUserId, request);

        assertEquals("failed", result.status());
        assertEquals(1, result.targetCount());
        assertEquals(0, result.sentCount());
        assertEquals(1, result.failedCount());
        verify(notificationRepository).markDeliveryFailed(deliveryId, "registration token is not valid");
        verify(notificationRepository).markDeviceInvalid(target.deviceId());
        verify(notificationRepository).finishSend(notification.id(), "failed", 0, 1);
    }

    private AdminNotificationDto notification() {
        return notification("draft");
    }

    private AdminNotificationDto notification(String status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AdminNotificationDto(
            UUID.randomUUID(),
            "Update",
            "Please update Sanctuary.",
            "all",
            status,
            0,
            0,
            0,
            null,
            now,
            now
        );
    }

    private PushNotificationTarget target() {
        return new PushNotificationTarget(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            "ios",
            "fcm-token"
        );
    }
}
