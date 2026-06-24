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

    boolean notificationsEnabled,

    @Size(max = 128)
    @Pattern(regexp = "[A-Za-z0-9._:-]+")
    String clientInstanceId,

    Boolean automatedTest,

    @Size(max = 64)
    @Pattern(regexp = "app|automated_test|legacy")
    String checkInSource
) {}
