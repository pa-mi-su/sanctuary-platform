package app.sanctuary.api.asksanctuary.repository;

import java.util.UUID;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryStatus;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelUsage;

public record AskSanctuarySessionLog(
    UUID userId,
    String inputMessage,
    String inputHash,
    AskSanctuaryIntent intent,
    AskSanctuaryGuardrailType guardrailType,
    boolean guardrailTriggered,
    AskSanctuaryStatus status,
    AskSanctuaryResponse response,
    String responsePayload,
    boolean reusedResponse,
    AskSanctuaryModelUsage classificationUsage,
    AskSanctuaryModelUsage generationUsage
) {
}
