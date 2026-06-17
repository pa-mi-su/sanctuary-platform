package app.sanctuary.api.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserDeviceRegistrationRequest(
    @NotBlank
    @Size(max = 4096)
    String fcmToken,

    @NotBlank
    @Pattern(regexp = "ios|android")
    String platform,

    @Size(max = 64)
    String appVersion,

    @NotBlank
    @Pattern(regexp = "en|es|pl")
    String language,

    boolean notificationsEnabled
) {}
