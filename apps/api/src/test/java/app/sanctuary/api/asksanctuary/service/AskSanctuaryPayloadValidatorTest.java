package app.sanctuary.api.asksanctuary.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryGuardrailDto;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.dto.ScriptureReferenceDto;

class AskSanctuaryPayloadValidatorTest {

    private final AskSanctuaryPayloadValidator validator = new AskSanctuaryPayloadValidator();

    @Test
    void acceptsCompleteNormalPayload() {
        assertThatCode(() -> validator.validate(validResponse()))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingScriptureReferenceFields() {
        AskSanctuaryResponse response = new AskSanctuaryResponse(
            "OK",
            false,
            false,
            null,
            null,
            "Courage for today",
            new ScriptureReferenceDto("Isaiah", "", "10"),
            new ScriptureReferenceDto("Matthew", "11", "28"),
            "St. Joseph",
            "Our Father",
            "Bring the day to prayer.",
            "Pray slowly for one minute.",
            "WORK_OR_DISCERNMENT",
            new AskSanctuaryGuardrailDto("NONE", false)
        );

        assertThatThrownBy(() -> validator.validate(response))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("oldTestament.chapter");
    }

    @Test
    void rejectsMissingGuardrailMetadata() {
        AskSanctuaryResponse response = new AskSanctuaryResponse(
            "OK",
            false,
            false,
            null,
            null,
            "Courage for today",
            new ScriptureReferenceDto("Isaiah", "41", "10"),
            new ScriptureReferenceDto("Matthew", "11", "28"),
            "St. Joseph",
            "Our Father",
            "Bring the day to prayer.",
            "Pray slowly for one minute.",
            "WORK_OR_DISCERNMENT",
            null
        );

        assertThatThrownBy(() -> validator.validate(response))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("guardrail");
    }

    private AskSanctuaryResponse validResponse() {
        return new AskSanctuaryResponse(
            "OK",
            false,
            false,
            null,
            null,
            "Courage for today",
            new ScriptureReferenceDto("Isaiah", "41", "10"),
            new ScriptureReferenceDto("Matthew", "11", "28"),
            "St. Joseph",
            "Our Father",
            "Bring the day to prayer.",
            "Pray slowly for one minute.",
            "WORK_OR_DISCERNMENT",
            new AskSanctuaryGuardrailDto("NONE", false)
        );
    }
}
