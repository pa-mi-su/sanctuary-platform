package app.sanctuary.api.admin.config;

import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.admin")
public record AdminProperties(
    List<String> bootstrapEmails
) {
    public boolean isBootstrapAdmin(String email) {
        if (email == null || email.isBlank() || bootstrapEmails == null || bootstrapEmails.isEmpty()) {
            return false;
        }

        String normalizedEmail = normalize(email);
        return bootstrapEmails.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(AdminProperties::normalize)
            .anyMatch(normalizedEmail::equals);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
