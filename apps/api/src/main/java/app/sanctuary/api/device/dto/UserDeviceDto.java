package app.sanctuary.api.device.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserDeviceDto(
    UUID id,
    UUID userId,
    String platform,
    String appVersion,
    String language,
    boolean notificationsEnabled,
    String tokenStatus,
    OffsetDateTime lastSeenAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
