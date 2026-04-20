package app.sanctuary.api.content.support;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ContentRequestValidatorTest {

    private ContentRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ContentRequestValidator();
    }

    @Test
    void acceptsValidMonthDay() {
        assertDoesNotThrow(() -> validator.validateMonthDay(4, 19));
    }

    @Test
    void rejectsInvalidMonthDay() {
        assertThrows(ResponseStatusException.class, () -> validator.validateMonthDay(0, 10));
        assertThrows(ResponseStatusException.class, () -> validator.validateMonthDay(4, 32));
    }

    @Test
    void acceptsBoundedDateRange() {
        assertDoesNotThrow(() -> validator.validateDateRange(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 5, 31),
            62
        ));
    }

    @Test
    void rejectsInvertedOrOversizedDateRange() {
        assertThrows(ResponseStatusException.class, () -> validator.validateDateRange(
            LocalDate.of(2026, 4, 2),
            LocalDate.of(2026, 4, 1),
            62
        ));

        assertThrows(ResponseStatusException.class, () -> validator.validateDateRange(
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 6, 2),
            62
        ));
    }
}
