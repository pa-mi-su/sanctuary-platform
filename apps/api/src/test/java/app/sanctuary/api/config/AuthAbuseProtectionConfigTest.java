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
        });
    }
    @Test
    void bindsExplicitTrustedProxyRanges() {
        contextRunner.withPropertyValues("sanctuary.auth.abuse.trusted-proxy-cidrs[0]=172.30.0.0/24")
            .run(context -> assertThat(context.getBean(AuthAbuseProtectionProperties.class).getTrustedProxyCidrs())
                .containsExactly("172.30.0.0/24"));
    }
}
