package app.sanctuary.api.admin.dto;

public record AdminUserMetricsDto(
    int totalUsers,
    int activeUsersToday,
    int activeUsers7Days,
    int activeUsers30Days,
    int deviceCount,
    int activeDevices7Days,
    int activeDevices30Days,
    int iosDeviceCount,
    int androidDeviceCount,
    int englishDeviceCount,
    int spanishDeviceCount,
    int polishDeviceCount,
    int notificationsEnabledDeviceCount,
    int invalidTokenCount,
    int unknownAppVersionDeviceCount
) {}
