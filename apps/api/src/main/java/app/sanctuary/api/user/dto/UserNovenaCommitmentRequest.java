package app.sanctuary.api.user.dto;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserNovenaCommitmentRequest(
    OffsetDateTime startedAt,
    @Min(1) int currentDay,
    @NotNull List<@Min(1) Integer> completedDays,
    boolean reminderEnabled,
    Integer reminderMorningHour,
    Integer reminderEveningHour,
    @NotBlank String reminderTimeZoneId,
    @NotBlank String status
) {
}
