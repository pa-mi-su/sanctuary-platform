package app.sanctuary.api.asksanctuary.repository;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryGuardrailDto;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.dto.ScriptureReferenceDto;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryStatus;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelUsage;

@ExtendWith(MockitoExtension.class)
class AskSanctuaryRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void savePersistsSessionSummaryAndJsonPayload() {
        AskSanctuaryRepository repository = new AskSanctuaryRepository(jdbcTemplate, new ObjectMapper());
        UUID userId = UUID.randomUUID();
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
            new AskSanctuaryGuardrailDto("NONE", false)
        );

        repository.save(new AskSanctuarySessionLog(
            userId,
            "I have a job interview tomorrow.",
            "input-hash",
            AskSanctuaryIntent.WORK_OR_DISCERNMENT,
            AskSanctuaryGuardrailType.NONE,
            false,
            AskSanctuaryStatus.OK,
            response,
            "{\"status\":\"OK\"}",
            false,
            new AskSanctuaryModelUsage("gpt-4.1-nano", 22, 6, 28),
            new AskSanctuaryModelUsage("gpt-4o-mini", 180, 220, 400)
        ));

        verify(jdbcTemplate).update(eq("""
                INSERT INTO ask_sanctuary_sessions (
                    user_id,
                    input_message,
                    detected_intent,
                    guardrail_type,
                    guardrail_triggered,
                    response_status,
                    response_payload,
                    input_hash,
                    reused_response,
                    classification_model,
                    classification_input_tokens,
                    classification_output_tokens,
                    classification_total_tokens,
                    generation_model,
                    generation_input_tokens,
                    generation_output_tokens,
                    generation_total_tokens,
                    old_testament_reference,
                    new_testament_reference,
                    saint,
                    prayer
                )
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """),
            eq(userId),
            isNull(),
            eq("WORK_OR_DISCERNMENT"),
            eq("NONE"),
            eq(false),
            eq("OK"),
            eq("{\"status\":\"OK\"}"),
            eq("input-hash"),
            eq(false),
            eq("gpt-4.1-nano"),
            eq(22),
            eq(6),
            eq(28),
            eq("gpt-4o-mini"),
            eq(180),
            eq(220),
            eq(400),
            eq("Isaiah 41:10"),
            eq("Matthew 11:28"),
            eq("St. Joseph"),
            eq("Our Father")
        );
    }
}
