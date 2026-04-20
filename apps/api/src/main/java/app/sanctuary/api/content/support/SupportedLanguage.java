package app.sanctuary.api.content.support;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum SupportedLanguage {
    EN("en"),
    ES("es"),
    PL("pl");

    private final String code;

    SupportedLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static SupportedLanguage from(String value) {
        if (value == null || value.isBlank()) {
            return EN;
        }

        return switch (value.toLowerCase(Locale.US)) {
            case "en" -> EN;
            case "es" -> ES;
            case "pl" -> PL;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language: " + value);
        };
    }
}
