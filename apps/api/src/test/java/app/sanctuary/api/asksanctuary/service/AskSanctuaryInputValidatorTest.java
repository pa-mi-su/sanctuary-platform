package app.sanctuary.api.asksanctuary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AskSanctuaryInputValidatorTest {

    private final AskSanctuaryInputValidator validator = new AskSanctuaryInputValidator();

    @Test
    void acceptsOneSimpleFeelingWord() {
        assertThat(validator.validate("worried").normalizedMessage())
            .isEqualTo("worried");
        assertThat(validator.validate("burned-out").normalizedMessage())
            .isEqualTo("burned-out");
    }

    @Test
    void rejectsSentencesAndMultipleWords() {
        assertThatThrownBy(() -> validator.validate("I feel tired today"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("one simple feeling word");
        assertThatThrownBy(() -> validator.validate("worried tired"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("one simple feeling word");
    }

    @Test
    void rejectsSymbolsAndBlockedWords() {
        assertThatThrownBy(() -> validator.validate("sad!"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("poop"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("fuck"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
