package app.sanctuary.api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.admin.abuse")
public class AdminAbuseProtectionProperties {

    private boolean enabled = true;
    private Limit requestLimit = new Limit(60, Duration.ofMinutes(1));

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Limit requestLimit() {
        return requestLimit;
    }

    public void setRequestLimit(Limit requestLimit) {
        this.requestLimit = requestLimit;
    }

    public record Limit(int maxAttempts, Duration window) {
    }
}
