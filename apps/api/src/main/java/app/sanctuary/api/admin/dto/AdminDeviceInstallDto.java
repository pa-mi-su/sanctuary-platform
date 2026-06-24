package app.sanctuary.api.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminDeviceInstallDto(
    String id,
    UUID userId,
    String userEmail,
    String userDisplayName,
    boolean signedIn,
    String platform,
    String appVersion,
    String language,
    boolean notificationsEnabled,
    String tokenStatus,
    boolean hasPushToken,
    boolean pushReady,
    String clientInstanceId,
    String checkInSource,
    OffsetDateTime firstSeenAt,
    OffsetDateTime lastSeenAt
) {}
