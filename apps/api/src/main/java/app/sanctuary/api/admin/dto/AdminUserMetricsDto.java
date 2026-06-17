package app.sanctuary.api.admin.dto;

public record AdminUserMetricsDto(
    int totalUsers,
    int deviceCount,
    int iosDeviceCount,
    int androidDeviceCount,
    int notificationsEnabledDeviceCount,
    int invalidTokenCount
) {}
