package app.sanctuary.api.asksanctuary.openai;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;

public record AskSanctuaryModelRequest(
    String message,
    AskSanctuaryIntent intent,
    boolean retry
) {
}
