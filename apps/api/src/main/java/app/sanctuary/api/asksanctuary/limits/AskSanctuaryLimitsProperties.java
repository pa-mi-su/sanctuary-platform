package app.sanctuary.api.asksanctuary.limits;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.ask-sanctuary.limits")
public record AskSanctuaryLimitsProperties(
    int freeDailyLimit,
    int plusDailyLimit,
    int ipDailyLimit,
    int burstLimit,
    Duration burstWindow,
    int misuseThreshold,
    Duration misuseWindow,
    Duration misuseLockDuration
) {
    public AskSanctuaryLimitsProperties {
        if (freeDailyLimit < 0) {
            freeDailyLimit = 3;
        }
        if (plusDailyLimit < 0) {
            plusDailyLimit = 50;
        }
        if (ipDailyLimit <= 0) {
            ipDailyLimit = 100;
        }
        if (burstLimit <= 0) {
            burstLimit = 10;
        }
        if (burstWindow == null || burstWindow.isNegative() || burstWindow.isZero()) {
            burstWindow = Duration.ofMinutes(1);
        }
        if (misuseThreshold <= 0) {
            misuseThreshold = 3;
        }
        if (misuseWindow == null || misuseWindow.isNegative() || misuseWindow.isZero()) {
            misuseWindow = Duration.ofHours(24);
        }
        if (misuseLockDuration == null || misuseLockDuration.isNegative() || misuseLockDuration.isZero()) {
            misuseLockDuration = Duration.ofHours(24);
        }
    }
}
