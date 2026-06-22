package app.sanctuary.api.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationDeliveryDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;
import app.sanctuary.api.admin.dto.AdminNotificationSendResultDto;
import app.sanctuary.api.admin.notification.PushNotificationGateway;
import app.sanctuary.api.admin.notification.PushNotificationPayload;
import app.sanctuary.api.admin.notification.PushNotificationSendResult;
import app.sanctuary.api.admin.repository.AdminAuditRepository;
import app.sanctuary.api.admin.repository.AdminNotificationRepository;

@Service
public class AdminNotificationService {

    private static final int MAX_LIMIT = 250;

    private final AdminNotificationRepository notificationRepository;
    private final AdminAuditRepository auditRepository;
    private final PushNotificationGateway pushNotificationGateway;

    public AdminNotificationService(
        AdminNotificationRepository notificationRepository,
        AdminAuditRepository auditRepository,
        PushNotificationGateway pushNotificationGateway
    ) {
        this.notificationRepository = notificationRepository;
        this.auditRepository = auditRepository;
        this.pushNotificationGateway = pushNotificationGateway;
    }

    public AdminNotificationDto createDraft(UUID adminUserId, AdminNotificationRequest request) {
        AdminNotificationDto notification = notificationRepository.createDraft(adminUserId, request);
        auditRepository.record(adminUserId, "admin.notification.create_draft", "admin_notification", notification.id().toString());
        return notification;
    }

    public List<AdminNotificationDto> history(int requestedLimit) {
        int limit = requestedLimit <= 0 ? 50 : Math.min(requestedLimit, MAX_LIMIT);
        return notificationRepository.history(limit);
    }

    public List<AdminNotificationDeliveryDto> recentDeliveries(int requestedLimit) {
        int limit = requestedLimit <= 0 ? 50 : Math.min(requestedLimit, MAX_LIMIT);
        return notificationRepository.recentDeliveries(limit);
    }

    @Transactional
    public AdminNotificationSendResultDto send(UUID adminUserId, UUID notificationId) {
        if (!pushNotificationGateway.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Firebase notifications are not configured.");
        }

        AdminNotificationDto notification = notificationRepository.findById(notificationId);
        if (notification == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification draft was not found.");
        }
        if (!"draft".equals(notification.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft notifications can be sent.");
        }

        var targets = notificationRepository.findValidTargetsForAllAudience();
        if (targets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No push-ready devices are available.");
        }

        if (!notificationRepository.markSending(notificationId, adminUserId, targets.size())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Notification is no longer available to send.");
        }

        int sentCount = 0;
        int failedCount = 0;
        PushNotificationPayload payload = new PushNotificationPayload(
            notification.id(),
            notification.title(),
            notification.message()
        );

        for (var target : targets) {
            UUID deliveryId = notificationRepository.createDelivery(notificationId, target);
            PushNotificationSendResult result = pushNotificationGateway.send(target, payload);
            if (result.sent()) {
                sentCount += 1;
                notificationRepository.markDeliverySent(deliveryId);
            } else {
                failedCount += 1;
                notificationRepository.markDeliveryFailed(deliveryId, result.failureReason());
                if (result.invalidToken()) {
                    notificationRepository.markDeviceInvalid(target.deviceId());
                    notificationRepository.markAnonymousDeviceInvalid(target.anonymousDeviceId());
                }
            }
        }

        String finalStatus = failedCount > 0 && sentCount == 0 ? "failed" : "sent";
        notificationRepository.finishSend(notificationId, finalStatus, sentCount, failedCount);
        auditRepository.record(adminUserId, "admin.notification.send", "admin_notification", notificationId.toString());
        return new AdminNotificationSendResultDto(notificationId, finalStatus, targets.size(), sentCount, failedCount);
    }
}
