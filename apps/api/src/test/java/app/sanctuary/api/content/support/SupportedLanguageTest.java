package app.sanctuary.api.content.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SupportedLanguageTest {

    @Test
    void defaultsToEnglishWhenMissing() {
        assertEquals(SupportedLanguage.EN, SupportedLanguage.from(null));
        assertEquals(SupportedLanguage.EN, SupportedLanguage.from(""));
        assertEquals(SupportedLanguage.EN, SupportedLanguage.from("   "));
    }

    @Test
    void acceptsSupportedLanguagesCaseInsensitively() {
        assertEquals(SupportedLanguage.EN, SupportedLanguage.from("EN"));
        assertEquals(SupportedLanguage.ES, SupportedLanguage.from("es"));
        assertEquals(SupportedLanguage.PL, SupportedLanguage.from("Pl"));
    }

    @Test
    void rejectsUnsupportedLanguagesAsBadRequest() {
        assertThrows(ResponseStatusException.class, () -> SupportedLanguage.from("fr"));
    }
}
