package app.sanctuary.api.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserListItemDto(
    UUID userId,
    String email,
    String displayName,
    String preferredLanguage,
    OffsetDateTime registrationDate,
    OffsetDateTime lastSignInAt,
    int deviceCount,
    String latestPlatform,
    String latestAppVersion,
    String latestDeviceLanguage,
    OffsetDateTime latestDeviceLastSeenAt,
    boolean notificationsEnabled
) {}
