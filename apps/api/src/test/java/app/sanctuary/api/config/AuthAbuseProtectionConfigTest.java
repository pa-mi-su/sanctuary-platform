package app.sanctuary.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuthAbuseProtectionConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(AuthAbuseProtectionConfig.class)
        .withBean(ObjectMapper.class);

    @Test
    void createsSpringManagedAuthAbuseProtectionFilter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AuthAbuseProtectionProperties.class);
            assertThat(context).hasSingleBean(AuthAbuseProtectionFilter.class);
            assertThat(context).hasSingleBean(AdminAbuseProtectionProperties.class);
            assertThat(context).hasSingleBean(AdminAbuseProtectionFilter.class);
        });
    }
}
