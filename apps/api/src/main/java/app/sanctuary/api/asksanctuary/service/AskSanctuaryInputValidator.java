package app.sanctuary.api.asksanctuary.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class AskSanctuaryInputValidator {

    private static final int REQUIRED_WORDS = 1;
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("^[\\p{L}\\s,'-]+$");
    private static final Pattern WORD = Pattern.compile("^[\\p{L}][\\p{L}'-]{1,23}$");
    private static final Set<String> BLOCKED_WORDS = Set.of(
        "ass",
        "bullshit",
        "crap",
        "fart",
        "fuck",
        "fucked",
        "fucking",
        "nigger",
        "pee",
        "poo",
        "poop",
        "pooped",
        "pooping",
        "shit",
        "shitting"
    );

    public AskSanctuaryInput validate(String message) {
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isBlank()) {
            throw invalid();
        }
        if (!ALLOWED_CHARACTERS.matcher(trimmed).matches()) {
            throw invalid();
        }

        List<String> words = Arrays.stream(trimmed.replace(',', ' ').split("\\s+"))
            .map(word -> word.toLowerCase(Locale.ROOT))
            .filter(word -> !word.isBlank())
            .toList();

        if (words.size() != REQUIRED_WORDS) {
            throw invalid();
        }

        for (String word : words) {
            if (!WORD.matcher(word).matches() || BLOCKED_WORDS.contains(word)) {
                throw invalid();
            }
        }

        return new AskSanctuaryInput(words, String.join(", ", words));
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("Sanctuary Companion works best with one simple feeling word.");
    }
}
