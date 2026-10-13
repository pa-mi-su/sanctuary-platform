package app.sanctuary.api.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoClientConfig {

    @Bean(destroyMethod = "close")
    @Primary
    CognitoIdentityProviderClient cognitoIdentityProviderClient(AuthProperties authProperties) {
        return CognitoIdentityProviderClient.builder()
            .region(resolveRegion(authProperties.userPoolId()))
            .build();
    }

    // Separate SDK connection pool and timeouts for administrator membership/MFA calls.
    // Consumer login, refresh and account deletion retain their existing client settings.
    @Bean(name = "newsCognitoClient", destroyMethod = "close")
    CognitoIdentityProviderClient newsCognitoClient(AuthProperties authProperties) {
        return CognitoIdentityProviderClient.builder()
            .region(resolveRegion(authProperties.userPoolId()))
            .overrideConfiguration(configuration -> configuration
                .apiCallTimeout(java.time.Duration.ofSeconds(10))
                .apiCallAttemptTimeout(java.time.Duration.ofSeconds(4)))
            .build();
    }

    private Region resolveRegion(String userPoolId) {
        if (userPoolId == null || userPoolId.isBlank() || !userPoolId.contains("_")) {
            return Region.US_EAST_1;
        }

        return Region.of(userPoolId.substring(0, userPoolId.indexOf('_')));
    }
}
