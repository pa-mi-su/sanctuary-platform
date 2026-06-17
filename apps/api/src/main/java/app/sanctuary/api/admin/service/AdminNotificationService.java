package app.sanctuary.api.admin.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import app.sanctuary.api.admin.dto.AdminNotificationDto;
import app.sanctuary.api.admin.dto.AdminNotificationRequest;
import app.sanctuary.api.admin.repository.AdminAuditRepository;
import app.sanctuary.api.admin.repository.AdminNotificationRepository;

@Service
public class AdminNotificationService {

    private static final int MAX_LIMIT = 250;

    private final AdminNotificationRepository notificationRepository;
    private final AdminAuditRepository auditRepository;

    public AdminNotificationService(
        AdminNotificationRepository notificationRepository,
        AdminAuditRepository auditRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.auditRepository = auditRepository;
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
}
