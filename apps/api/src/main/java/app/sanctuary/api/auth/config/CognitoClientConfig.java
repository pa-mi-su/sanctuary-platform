package app.sanctuary.api.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import app.sanctuary.api.config.AuthProperties;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoClientConfig {

    @Bean(destroyMethod = "close")
    CognitoIdentityProviderClient cognitoIdentityProviderClient(AuthProperties authProperties) {
        return CognitoIdentityProviderClient.builder()
            .region(resolveRegion(authProperties.userPoolId()))
            .build();
    }

    private Region resolveRegion(String userPoolId) {
        if (userPoolId == null || userPoolId.isBlank() || !userPoolId.contains("_")) {
            return Region.US_EAST_1;
        }

        return Region.of(userPoolId.substring(0, userPoolId.indexOf('_')));
    }
}
