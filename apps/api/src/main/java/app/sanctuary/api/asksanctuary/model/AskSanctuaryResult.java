package app.sanctuary.api.asksanctuary.model;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelUsage;

public record AskSanctuaryResult(
    AskSanctuaryStatus status,
    AskSanctuaryIntent intent,
    AskSanctuaryGuardrailType guardrailType,
    boolean guardrailTriggered,
    AskSanctuaryResponse response,
    boolean reusedResponse,
    AskSanctuaryModelUsage classificationUsage,
    AskSanctuaryModelUsage generationUsage
) {
    public AskSanctuaryResult(
        AskSanctuaryStatus status,
        AskSanctuaryIntent intent,
        AskSanctuaryGuardrailType guardrailType,
        boolean guardrailTriggered,
        AskSanctuaryResponse response
    ) {
        this(
            status,
            intent,
            guardrailType,
            guardrailTriggered,
            response,
            false,
            AskSanctuaryModelUsage.none(),
            AskSanctuaryModelUsage.none()
        );
    }
}
