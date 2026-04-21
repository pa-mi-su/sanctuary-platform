package app.sanctuary.api.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserPreferenceDto(
    UUID userId,
    String timeZoneId,
    boolean novenaRemindersEnabled,
    boolean feastRemindersEnabled,
    boolean emailUpdatesEnabled,
    boolean onboardingCompleted,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
