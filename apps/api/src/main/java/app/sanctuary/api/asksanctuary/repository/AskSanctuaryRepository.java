package app.sanctuary.api.asksanctuary.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryStatus;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelUsage;

@Repository
public class AskSanctuaryRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AskSanctuaryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<AskSanctuaryCachedResponse> findRecentReusableResponse(UUID userId, String inputHash, OffsetDateTime cutoff) {
        if (inputHash == null || inputHash.isBlank()) {
            return Optional.empty();
        }

        return jdbcTemplate.query(
            """
                SELECT
                    detected_intent,
                    guardrail_type,
                    guardrail_triggered,
                    response_status,
                    response_payload::text
                FROM ask_sanctuary_sessions
                WHERE user_id = ?
                  AND input_hash = ?
                  AND response_status IN ('OK', 'FALLBACK')
                  AND created_at >= ?
                ORDER BY created_at DESC
                LIMIT 1
                """,
            (rs, rowNum) -> {
                try {
                    return new AskSanctuaryCachedResponse(
                        AskSanctuaryIntent.valueOf(rs.getString("detected_intent")),
                        AskSanctuaryGuardrailType.valueOf(rs.getString("guardrail_type")),
                        rs.getBoolean("guardrail_triggered"),
                        AskSanctuaryStatus.valueOf(rs.getString("response_status")),
                        objectMapper.readValue(rs.getString("response_payload"), AskSanctuaryResponse.class)
                    );
                } catch (JsonProcessingException exception) {
                    throw new IllegalStateException("Stored Ask Sanctuary response payload is invalid.", exception);
                }
            },
            userId,
            inputHash,
            cutoff
        ).stream().findFirst();
    }

    public void save(AskSanctuarySessionLog log) {
        jdbcTemplate.update(
            """
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
                """,
            log.userId(),
            null,
            log.intent().name(),
            log.guardrailType().name(),
            log.guardrailTriggered(),
            log.status().name(),
            log.responsePayload(),
            log.inputHash(),
            log.reusedResponse(),
            usage(log.classificationUsage()).model(),
            usage(log.classificationUsage()).inputTokens(),
            usage(log.classificationUsage()).outputTokens(),
            usage(log.classificationUsage()).totalTokens(),
            usage(log.generationUsage()).model(),
            usage(log.generationUsage()).inputTokens(),
            usage(log.generationUsage()).outputTokens(),
            usage(log.generationUsage()).totalTokens(),
            referenceText(log.response().oldTestament()),
            referenceText(log.response().newTestament()),
            log.response().saint(),
            log.response().prayer()
        );
    }

    private AskSanctuaryModelUsage usage(AskSanctuaryModelUsage usage) {
        return usage == null ? AskSanctuaryModelUsage.none() : usage;
    }

    private String referenceText(app.sanctuary.api.asksanctuary.dto.ScriptureReferenceDto reference) {
        if (reference == null) {
            return null;
        }
        return "%s %s:%s".formatted(reference.book(), reference.chapter(), reference.verse());
    }
}
