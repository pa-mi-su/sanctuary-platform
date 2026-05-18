package app.sanctuary.api.config;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.auth.abuse")
public class AuthAbuseProtectionProperties {

    private boolean enabled = true;
    private Limit register = new Limit(5, Duration.ofHours(1));
    private Limit login = new Limit(20, Duration.ofMinutes(15));
    private Limit forgotPassword = new Limit(5, Duration.ofHours(1));
    private Limit resendConfirmation = new Limit(5, Duration.ofHours(1));
    private Set<String> blockedSignupDomains = new LinkedHashSet<>(Set.of(
        "example.com",
        "example.org",
        "example.net",
        "test.com",
        "invalid.com"
    ));

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Limit register() {
        return register;
    }

    public void setRegister(Limit register) {
        this.register = register;
    }

    public Limit login() {
        return login;
    }

    public void setLogin(Limit login) {
        this.login = login;
    }

    public Limit forgotPassword() {
        return forgotPassword;
    }

    public void setForgotPassword(Limit forgotPassword) {
        this.forgotPassword = forgotPassword;
    }

    public Limit resendConfirmation() {
        return resendConfirmation;
    }

    public void setResendConfirmation(Limit resendConfirmation) {
        this.resendConfirmation = resendConfirmation;
    }

    public Set<String> blockedSignupDomains() {
        return blockedSignupDomains;
    }

    public void setBlockedSignupDomains(Set<String> blockedSignupDomains) {
        this.blockedSignupDomains = blockedSignupDomains == null ? Set.of() : blockedSignupDomains;
    }

    public record Limit(int maxAttempts, Duration window) {
    }
}
