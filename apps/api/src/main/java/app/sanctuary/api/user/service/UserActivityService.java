package app.sanctuary.api.user.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import app.sanctuary.api.user.dto.UserStreakSummaryDto;
import app.sanctuary.api.user.repository.UserActivityRepository;

@Service
public class UserActivityService {

    private final UserActivityRepository repository;

    public UserActivityService(UserActivityRepository repository) {
        this.repository = repository;
    }

    public void recordNovenaProgressActivity(UUID userId, String novenaId, String timeZoneId) {
        ZoneId zoneId = resolveZoneId(timeZoneId);
        OffsetDateTime occurredAt = OffsetDateTime.now(zoneId);
        repository.recordActivity(
            userId,
            "novena_progress",
            "novena",
            novenaId,
            occurredAt.toLocalDate(),
            occurredAt.withOffsetSameInstant(ZoneOffset.UTC)
        );
    }

    public UserStreakSummaryDto streakSummary(UUID userId, String timeZoneId) {
        List<LocalDate> activityDates = repository.findDistinctActivityDates(userId);
        if (activityDates.isEmpty()) {
            return new UserStreakSummaryDto(0, 0, null);
        }

        int longest = 1;
        int running = 1;
        for (int index = 1; index < activityDates.size(); index++) {
            LocalDate current = activityDates.get(index);
            LocalDate previous = activityDates.get(index - 1);
            if (previous.plusDays(1).equals(current)) {
                running++;
            } else {
                running = 1;
            }
            longest = Math.max(longest, running);
        }

        ZoneId zoneId = resolveZoneId(timeZoneId);
        LocalDate today = LocalDate.now(zoneId);
        LocalDate cursor = activityDates.get(activityDates.size() - 1);
        int currentStreak = 0;
        if (cursor != null && cursor.equals(today)) {
            currentStreak = 1;
            for (int index = activityDates.size() - 2; index >= 0; index--) {
                LocalDate previous = activityDates.get(index);
                if (previous.plusDays(1).equals(cursor)) {
                    currentStreak++;
                    cursor = previous;
                } else {
                    break;
                }
            }
        }

        return new UserStreakSummaryDto(currentStreak, longest, activityDates.get(activityDates.size() - 1));
    }

    private ZoneId resolveZoneId(String timeZoneId) {
        try {
            return timeZoneId == null || timeZoneId.isBlank() ? ZoneOffset.UTC : ZoneId.of(timeZoneId);
        } catch (Exception ignored) {
            return ZoneOffset.UTC;
        }
    }
}
