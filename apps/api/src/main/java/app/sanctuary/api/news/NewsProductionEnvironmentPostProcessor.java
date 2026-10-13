package app.sanctuary.api.news;

import app.sanctuary.api.config.AuthProperties;
import app.sanctuary.api.config.WebProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;

/** Validate after config loading but before DataSource/Flyway can connect or migrate. */
public class NewsProductionEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override public int getOrder() { return ConfigDataEnvironmentPostProcessor.ORDER + 1; }

    @Override public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) return;
        var binder=Binder.get(environment);
        var auth=binder.bind("sanctuary.auth",AuthProperties.class).orElseGet(AuthProperties::new);
        var web=binder.bind("sanctuary.web",WebProperties.class).orElseGet(WebProperties::new);
        new NewsDeploymentSafety(auth,web,environment.getProperty("spring.datasource.url",""));
        String key=environment.getProperty("sanctuary.news.challenge-key","");
        try {
            byte[] decoded=key.matches("[0-9a-fA-F]{64}")?java.util.HexFormat.of().parseHex(key):java.util.Base64.getDecoder().decode(key);
            if(decoded.length!=32)throw new IllegalArgumentException();
        } catch(IllegalArgumentException exception) {
            throw new IllegalStateException("Production requires a 32-byte private news challenge key");
        }
    }
}
