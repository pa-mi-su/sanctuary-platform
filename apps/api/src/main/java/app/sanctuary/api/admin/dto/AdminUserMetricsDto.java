package app.sanctuary.api.admin.dto;

public record AdminUserMetricsDto(
    int totalUsers,
    int registeredUsersToday,
    int activeUsersToday,
    int activeUsers30Days,
    int anonymousActiveDevicesToday,
    int anonymousActiveDevices7Days,
    int knownAppInstallCount,
    int activeKnownDeviceCountRecent,
    int pushReadyIosDeviceCount,
    int pushReadyAndroidDeviceCount,
    int notificationsEnabledDeviceCount,
    int validTokenCount,
    int invalidTokenCount,
    int unknownAppVersionDeviceCount,
    int notificationTargetedCount,
    int notificationSentCount,
    int notificationFailedCount
) {}
