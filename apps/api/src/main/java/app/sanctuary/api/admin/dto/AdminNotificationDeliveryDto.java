package app.sanctuary.api.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminNotificationDeliveryDto(
    UUID id,
    UUID notificationId,
    String notificationTitle,
    UUID userDeviceId,
    String anonymousDeviceId,
    UUID userId,
    String platform,
    String status,
    String failureReason,
    OffsetDateTime sentAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
