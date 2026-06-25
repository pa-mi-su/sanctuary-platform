package app.sanctuary.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class WebConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(WebConfig.class);

    @Test
    void usesConfiguredAllowedOrigins() {
        contextRunner
            .withPropertyValues(
                "sanctuary.web.allowed-origins[0]=http://localhost:4200",
                "sanctuary.web.allowed-origins[1]=https://dev.mydailysanctuary.com"
            )
            .run(context -> {
                var webConfig = context.getBean(WebConfig.class);

                assertThat(webConfig.allowedOrigins()).containsExactly(
                    "http://localhost:4200",
                    "https://dev.mydailysanctuary.com"
                );
            });
    }

    @Test
    void fallsBackToProductionSafeDefaults() {
        contextRunner.run(context -> {
            var webConfig = context.getBean(WebConfig.class);

            assertThat(webConfig.allowedOrigins()).containsExactly(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "https://mydailysanctuary.com",
                "https://www.mydailysanctuary.com"
            );
        });
    }
}
