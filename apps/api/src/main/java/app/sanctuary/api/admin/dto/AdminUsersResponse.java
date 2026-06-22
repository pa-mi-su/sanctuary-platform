package app.sanctuary.api.admin.dto;

import java.util.List;

public record AdminUsersResponse(
    AdminUserMetricsDto metrics,
    List<AdminUserListItemDto> users,
    List<AdminDeviceInstallDto> recentDeviceInstalls
) {}
