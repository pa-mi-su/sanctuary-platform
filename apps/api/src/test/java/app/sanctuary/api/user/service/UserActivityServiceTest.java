package app.sanctuary.api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.sanctuary.api.user.repository.UserActivityRepository;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActivityRepository repository;

    @InjectMocks
    private UserActivityService service;

    @Test
    void streakSummaryCalculatesCurrentAndLongestStreaks() {
        UUID userId = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        when(repository.findDistinctActivityDates(userId)).thenReturn(
            List.of(
                today.minusDays(4),
                today.minusDays(3),
                today.minusDays(1),
                today
            )
        );

        var summary = service.streakSummary(userId, "UTC");

        assertEquals(2, summary.currentStreakDays());
        assertEquals(2, summary.longestStreakDays());
        assertEquals(today, summary.lastActiveDate());
    }
}
