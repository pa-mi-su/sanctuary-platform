package app.sanctuary.api.asksanctuary.repository;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryStatus;

public record AskSanctuaryCachedResponse(
    AskSanctuaryIntent intent,
    AskSanctuaryGuardrailType guardrailType,
    boolean guardrailTriggered,
    AskSanctuaryStatus status,
    AskSanctuaryResponse response
) {
}
