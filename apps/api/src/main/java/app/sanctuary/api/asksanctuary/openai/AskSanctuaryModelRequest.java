package app.sanctuary.api.asksanctuary.openai;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;

public record AskSanctuaryModelRequest(
    String message,
    String locale,
    AskSanctuaryIntent intent,
    boolean retry,
    String variationGuidance
) {
}
