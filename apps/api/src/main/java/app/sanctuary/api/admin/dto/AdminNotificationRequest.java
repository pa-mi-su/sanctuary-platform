package app.sanctuary.api.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminNotificationRequest(
    @NotBlank
    @Size(max = 120)
    String title,

    @NotBlank
    @Size(max = 500)
    String message
) {}
