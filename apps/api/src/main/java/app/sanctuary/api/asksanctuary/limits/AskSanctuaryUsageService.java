package app.sanctuary.api.asksanctuary.limits;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;

@Service
public class AskSanctuaryUsageService {

    private final AskSanctuaryUsageRepository repository;
    private final AskSanctuaryLimitsProperties properties;
    private final Clock clock;

    @Autowired
    public AskSanctuaryUsageService(
        AskSanctuaryUsageRepository repository,
        AskSanctuaryLimitsProperties properties
    ) {
        this(repository, properties, Clock.systemUTC());
    }

    AskSanctuaryUsageService(
        AskSanctuaryUsageRepository repository,
        AskSanctuaryLimitsProperties properties,
        Clock clock
    ) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public boolean isBurstLimited(UUID userId) {
        if (isUnlimited(repository.findEntitlement(userId))) {
            return false;
        }

        OffsetDateTime cutoff = OffsetDateTime.now(clock).minus(properties.burstWindow());
        int recentAttempts = repository.recordAttemptAndCountRecent(userId, cutoff);
        return recentAttempts > properties.burstLimit();
    }

    public boolean isMisuseLocked(UUID userId) {
        if (isUnlimited(repository.findEntitlement(userId))) {
            return false;
        }
        return repository.findActiveLockUntil(userId, OffsetDateTime.now(clock)).isPresent();
    }

    public AskSanctuaryMisuseDecision recordMisuse(UUID userId, AskSanctuaryGuardrailType guardrailType) {
        if (isUnlimited(repository.findEntitlement(userId))) {
            return new AskSanctuaryMisuseDecision(false, 0);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime cutoff = now.minus(properties.misuseWindow());
        int misuseCount = repository.recordMisuseAndCountRecent(userId, guardrailType.name(), cutoff);
        boolean locked = misuseCount >= properties.misuseThreshold();

        if (locked) {
            repository.createLock(userId, guardrailType.name(), now.plus(properties.misuseLockDuration()));
        }

        return new AskSanctuaryMisuseDecision(locked, misuseCount);
    }

    public AskSanctuaryQuotaDecision reserveDailyCompanionRequest(UUID userId) {
        AskSanctuaryEntitlement entitlement = repository.findEntitlement(userId);
        if (isUnlimited(entitlement)) {
            return AskSanctuaryQuotaDecision.unlimitedAllowed();
        }

        int dailyLimit = dailyLimitFor(entitlement);
        if (dailyLimit <= 0) {
            return AskSanctuaryQuotaDecision.denied(0);
        }

        LocalDate usageDate = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return repository.reserveDailyRequest(userId, usageDate, dailyLimit)
            .map(usedToday -> AskSanctuaryQuotaDecision.allowed(dailyLimit, usedToday))
            .orElseGet(() -> AskSanctuaryQuotaDecision.denied(dailyLimit));
    }

    public AskSanctuaryQuotaDecision reserveDailyIpRequest(UUID userId, String ipHash) {
        if (isUnlimited(repository.findEntitlement(userId))) {
            return AskSanctuaryQuotaDecision.unlimitedAllowed();
        }

        int dailyLimit = properties.ipDailyLimit();
        LocalDate usageDate = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return repository.reserveDailyIpRequest(ipHash, usageDate, dailyLimit)
            .map(usedToday -> AskSanctuaryQuotaDecision.allowed(dailyLimit, usedToday))
            .orElseGet(() -> AskSanctuaryQuotaDecision.denied(dailyLimit));
    }

    private int dailyLimitFor(AskSanctuaryEntitlement entitlement) {
        if (entitlement.dailyLimitOverride() != null) {
            return entitlement.dailyLimitOverride();
        }

        return switch (entitlement.tier()) {
            case "PLUS" -> properties.plusDailyLimit();
            default -> properties.freeDailyLimit();
        };
    }

    private boolean isUnlimited(AskSanctuaryEntitlement entitlement) {
        if (entitlement == null) {
            return false;
        }
        return entitlement.unlimited() || "ADMIN".equals(entitlement.tier());
    }
}
