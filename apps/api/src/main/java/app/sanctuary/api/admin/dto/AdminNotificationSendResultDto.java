package app.sanctuary.api.admin.dto;

import java.util.UUID;

public record AdminNotificationSendResultDto(
    UUID notificationId,
    String status,
    int targetCount,
    int sentCount,
    int failedCount
) {}
