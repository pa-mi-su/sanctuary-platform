package app.sanctuary.api.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPreferencesUpdateRequest(
    @NotBlank String timeZoneId,
    boolean novenaRemindersEnabled,
    boolean feastRemindersEnabled,
    boolean emailUpdatesEnabled,
    boolean onboardingCompleted
) {
}
