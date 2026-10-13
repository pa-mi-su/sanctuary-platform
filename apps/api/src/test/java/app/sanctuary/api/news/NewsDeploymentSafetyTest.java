package app.sanctuary.api.news;

import org.junit.jupiter.api.Test;
import app.sanctuary.api.config.*;
import static org.assertj.core.api.Assertions.*;

class NewsDeploymentSafetyTest {
    @Test void productionChecksRunBeforeBeanInitialization() {
        var environment=new org.springframework.mock.env.MockEnvironment();
        environment.setActiveProfiles("prod");
        assertThatThrownBy(()->new NewsProductionEnvironmentPostProcessor().postProcessEnvironment(environment,null))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("authentication");
    }
    @Test void databaseTlsCannotBeDisabledOrOverriddenByDuplicateParameters() {
        assertThatThrownBy(()->NewsDeploymentSafety.requireVerifiedDatabaseTls("jdbc:postgresql://db.example.org/news"))
            .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->NewsDeploymentSafety.requireVerifiedDatabaseTls("jdbc:postgresql://db.example.org/news?sslmode=verify-full&sslmode=disable&sslrootcert=/app/rds-ca.pem"))
            .isInstanceOf(IllegalStateException.class);
    }
    @Test void productionRejectsDisabledAuthInsecureCookiesAndWildcardOrigins() {
        var auth=new AuthProperties(true,"client","pool","client");var web=new WebProperties();
        web.setAllowedOrigins(java.util.List.of("https://mydailysanctuary.com"));
        assertThatCode(()->new NewsDeploymentSafety(auth,web,"jdbc:postgresql://db.example.org/news?sslmode=verify-full&sslrootcert=/app/rds-ca.pem")).doesNotThrowAnyException();
        auth.setEnabled(false);assertThatThrownBy(()->new NewsDeploymentSafety(auth,web,"jdbc:postgresql://db.example.org/news?sslmode=verify-full&sslrootcert=/app/rds-ca.pem")).isInstanceOf(IllegalStateException.class);
        auth.setEnabled(true);auth.cookie().setSecure(false);assertThatThrownBy(()->new NewsDeploymentSafety(auth,web,"jdbc:postgresql://db.example.org/news?sslmode=verify-full&sslrootcert=/app/rds-ca.pem")).isInstanceOf(IllegalStateException.class);
        auth.cookie().setSecure(true);web.setAllowedOrigins(java.util.List.of("https://*.example.org"));
        assertThatThrownBy(()->new NewsDeploymentSafety(auth,web,"jdbc:postgresql://db.example.org/news?sslmode=verify-full&sslrootcert=/app/rds-ca.pem")).isInstanceOf(IllegalStateException.class);
    }
}
