package app.sanctuary.api.asksanctuary.limits;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import app.sanctuary.api.asksanctuary.service.AskSanctuaryProperties;

@Configuration
@EnableConfigurationProperties({AskSanctuaryLimitsProperties.class, AskSanctuaryProperties.class})
public class AskSanctuaryLimitsConfig {
}
