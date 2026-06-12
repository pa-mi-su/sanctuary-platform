package app.sanctuary.api.asksanctuary.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.ask-sanctuary")
public record AskSanctuaryProperties(
    boolean enabled,
    Cache cache
) {
    public AskSanctuaryProperties {
        if (cache == null) {
            cache = new Cache(true, Duration.ofHours(24));
        }
    }

    public record Cache(
        boolean enabled,
        Duration window
    ) {
        public Cache {
            if (window == null || window.isNegative() || window.isZero()) {
                window = Duration.ofHours(24);
            }
        }
    }
}
