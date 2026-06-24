package app.sanctuary.api.activity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserAppActivityRequest(
    @Size(max = 128)
    @Pattern(regexp = "[A-Za-z0-9._:-]+")
    String anonymousDeviceId,

    @NotBlank
    @Pattern(regexp = "app_open|session_start|foreground_heartbeat")
    String eventType,

    @NotBlank
    @Pattern(regexp = "ios|android")
    String platform,

    @Size(max = 64)
    String appVersion,

    @NotBlank
    @Pattern(regexp = "en|es|pl")
    String language,

    @Size(max = 128)
    String timeZoneId,

    @Size(max = 128)
    @Pattern(regexp = "[A-Za-z0-9._:-]+")
    String clientInstanceId,

    Boolean automatedTest,

    @Size(max = 64)
    @Pattern(regexp = "app|automated_test|legacy")
    String checkInSource
) {}
