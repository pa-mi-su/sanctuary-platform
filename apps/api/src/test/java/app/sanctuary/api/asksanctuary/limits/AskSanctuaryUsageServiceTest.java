package app.sanctuary.api.asksanctuary.limits;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;

@ExtendWith(MockitoExtension.class)
class AskSanctuaryUsageServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-11T14:00:00Z"), ZoneOffset.UTC);

    @Mock
    private AskSanctuaryUsageRepository repository;

    @Test
    void reservesFreeDailyRequestWithFreeLimit() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(AskSanctuaryEntitlement.free());
        when(repository.reserveDailyRequest(userId, LocalDate.parse("2026-06-11"), 3))
            .thenReturn(Optional.of(3));

        AskSanctuaryQuotaDecision decision = service.reserveDailyCompanionRequest(userId);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.dailyLimit()).isEqualTo(3);
        assertThat(decision.usedToday()).isEqualTo(3);
    }

    @Test
    void deniesFreeDailyRequestWhenLimitIsAlreadyReached() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(AskSanctuaryEntitlement.free());
        when(repository.reserveDailyRequest(userId, LocalDate.parse("2026-06-11"), 3))
            .thenReturn(Optional.empty());

        AskSanctuaryQuotaDecision decision = service.reserveDailyCompanionRequest(userId);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.dailyLimit()).isEqualTo(3);
    }

    @Test
    void plusTierUsesPlusLimit() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(new AskSanctuaryEntitlement("PLUS", null, false));
        when(repository.reserveDailyRequest(userId, LocalDate.parse("2026-06-11"), 50))
            .thenReturn(Optional.of(10));

        AskSanctuaryQuotaDecision decision = service.reserveDailyCompanionRequest(userId);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.dailyLimit()).isEqualTo(50);
        assertThat(decision.usedToday()).isEqualTo(10);
    }

    @Test
    void adminTierBypassesDailyCounter() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(new AskSanctuaryEntitlement("ADMIN", null, false));

        AskSanctuaryQuotaDecision decision = service.reserveDailyCompanionRequest(userId);

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.unlimited()).isTrue();
        verify(repository, never()).reserveDailyRequest(any(), any(), any(Integer.class));
    }

    @Test
    void reservesIpDailyRequestWithIpLimit() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(AskSanctuaryEntitlement.free());
        when(repository.reserveDailyIpRequest("ip-hash", LocalDate.parse("2026-06-11"), 100))
            .thenReturn(Optional.of(7));

        AskSanctuaryQuotaDecision decision = service.reserveDailyIpRequest(userId, "ip-hash");

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.dailyLimit()).isEqualTo(100);
        assertThat(decision.usedToday()).isEqualTo(7);
    }

    @Test
    void deniesIpDailyRequestWhenLimitIsReached() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(AskSanctuaryEntitlement.free());
        when(repository.reserveDailyIpRequest("ip-hash", LocalDate.parse("2026-06-11"), 100))
            .thenReturn(Optional.empty());

        AskSanctuaryQuotaDecision decision = service.reserveDailyIpRequest(userId, "ip-hash");

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.dailyLimit()).isEqualTo(100);
    }

    @Test
    void detectsBurstLimitWhenRecentAttemptCountExceedsLimit() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.recordAttemptAndCountRecent(eq(userId), any())).thenReturn(11);

        assertThat(service.isBurstLimited(userId)).isTrue();
    }

    @Test
    void allowsBurstWhenRecentAttemptCountIsAtLimit() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.recordAttemptAndCountRecent(eq(userId), any())).thenReturn(10);

        assertThat(service.isBurstLimited(userId)).isFalse();
    }

    @Test
    void adminTierBypassesBurstLimitForTesting() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(new AskSanctuaryEntitlement("ADMIN", null, false));

        assertThat(service.isBurstLimited(userId)).isFalse();

        verify(repository, never()).recordAttemptAndCountRecent(any(), any());
    }

    @Test
    void activeMisuseLockReturnsTrue() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(AskSanctuaryEntitlement.free());
        when(repository.findActiveLockUntil(eq(userId), any())).thenReturn(Optional.of(OffsetDateTime.parse("2026-06-12T14:00:00Z")));

        assertThat(service.isMisuseLocked(userId)).isTrue();
    }

    @Test
    void thirdMisuseCreatesLock() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(AskSanctuaryEntitlement.free());
        when(repository.recordMisuseAndCountRecent(eq(userId), eq("IRRELEVANT"), any())).thenReturn(3);

        AskSanctuaryMisuseDecision decision = service.recordMisuse(userId, AskSanctuaryGuardrailType.IRRELEVANT);

        assertThat(decision.locked()).isTrue();
        assertThat(decision.recentMisuseCount()).isEqualTo(3);
        verify(repository).createLock(eq(userId), eq("IRRELEVANT"), eq(OffsetDateTime.parse("2026-06-12T14:00:00Z")));
    }

    @Test
    void secondMisuseDoesNotCreateLock() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(AskSanctuaryEntitlement.free());
        when(repository.recordMisuseAndCountRecent(eq(userId), eq("VIOLENCE_RISK"), any())).thenReturn(2);

        AskSanctuaryMisuseDecision decision = service.recordMisuse(userId, AskSanctuaryGuardrailType.VIOLENCE_RISK);

        assertThat(decision.locked()).isFalse();
        assertThat(decision.recentMisuseCount()).isEqualTo(2);
        verify(repository, never()).createLock(any(), any(), any());
    }

    @Test
    void adminTierBypassesMisuseLocksForTesting() {
        UUID userId = UUID.randomUUID();
        AskSanctuaryUsageService service = service();
        when(repository.findEntitlement(userId)).thenReturn(new AskSanctuaryEntitlement("ADMIN", null, false));

        assertThat(service.isMisuseLocked(userId)).isFalse();

        AskSanctuaryMisuseDecision decision = service.recordMisuse(userId, AskSanctuaryGuardrailType.IRRELEVANT);

        assertThat(decision.locked()).isFalse();
        verify(repository, never()).findActiveLockUntil(any(), any());
        verify(repository, never()).recordMisuseAndCountRecent(any(), any(), any());
        verify(repository, never()).createLock(any(), any(), any());
    }

    private AskSanctuaryUsageService service() {
        return new AskSanctuaryUsageService(
            repository,
            new AskSanctuaryLimitsProperties(3, 50, 100, 10, Duration.ofMinutes(1), 3, Duration.ofHours(24), Duration.ofHours(24)),
            CLOCK
        );
    }
}
