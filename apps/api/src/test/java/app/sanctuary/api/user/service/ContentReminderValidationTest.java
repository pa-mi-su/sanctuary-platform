package app.sanctuary.api.user.service;

import app.sanctuary.api.user.dto.ContentReminder;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentReminderValidationTest {
    @Test void requiresExplicitValidMinutesAndSafeContentIdentity() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new ContentReminder("novenas","st_cecilia","St. Cecilia",14,55)).isEmpty());
            for (var reminder : java.util.List.of(
                new ContentReminder("prayers","creed","Creed",null,0),
                new ContentReminder("prayers","creed","Creed",8,null),
                new ContentReminder("prayers","creed","Creed",24,0),
                new ContentReminder("prayers","creed","Creed",8,60),
                new ContentReminder("prayers","../creed","Creed",8,0),
                new ContentReminder("saints","cecilia","Cecilia",8,0))) {
                assertFalse(validator.validate(reminder).isEmpty());
            }
        }
    }
}
