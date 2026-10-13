package app.sanctuary.api.auth.config;

import java.time.Duration;
import app.sanctuary.api.config.AuthProperties;
import app.sanctuary.api.news.NewsAdminDirectory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoClientConfigTest {
    @Test void consumerRemainsPrimaryAndAdministratorHasSeparateConnectionPoolAndTimeouts() {
        new ApplicationContextRunner().withUserConfiguration(CognitoClientConfig.class)
            .withBean(AuthProperties.class, () -> new AuthProperties(true,"client","us-east-1_pool","client"))
            .withBean(NewsAdminDirectory.class)
            .run(context -> {
                assertThat(context).hasNotFailed();
                var consumer = context.getBean(CognitoIdentityProviderClient.class);
                var administrator = context.getBean("newsCognitoClient",CognitoIdentityProviderClient.class);
                assertThat(consumer).isSameAs(context.getBean("cognitoIdentityProviderClient")).isNotSameAs(administrator);
                var consumerSettings = consumer.serviceClientConfiguration().overrideConfiguration();
                var adminSettings = administrator.serviceClientConfiguration().overrideConfiguration();
                assertThat(consumerSettings.apiCallTimeout()).isEmpty();
                assertThat(consumerSettings.apiCallAttemptTimeout()).isEmpty();
                assertThat(adminSettings.apiCallTimeout()).contains(Duration.ofSeconds(10));
                assertThat(adminSettings.apiCallAttemptTimeout()).contains(Duration.ofSeconds(4));
            });
    }
}
