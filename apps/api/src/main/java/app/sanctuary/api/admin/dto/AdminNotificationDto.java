package app.sanctuary.api.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminNotificationDto(
    UUID id,
    String title,
    String message,
    String audienceType,
    String status,
    int targetCount,
    int deliveredCount,
    int failedCount,
    OffsetDateTime sentAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
