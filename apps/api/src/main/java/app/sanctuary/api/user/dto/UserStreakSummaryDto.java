package app.sanctuary.api.user.dto;

import java.time.LocalDate;

public record UserStreakSummaryDto(
    int currentStreakDays,
    int longestStreakDays,
    LocalDate lastActiveDate
) {
}
