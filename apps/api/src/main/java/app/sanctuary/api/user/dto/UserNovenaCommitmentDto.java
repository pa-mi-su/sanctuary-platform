package app.sanctuary.api.user.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UserNovenaCommitmentDto(
    String novenaId,
    OffsetDateTime startedAt,
    int currentDay,
    List<Integer> completedDays,
    boolean reminderEnabled,
    Integer reminderMorningHour,
    Integer reminderEveningHour,
    String reminderTimeZoneId,
    String status,
    OffsetDateTime updatedAt
) {
}
