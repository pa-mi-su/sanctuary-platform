package app.sanctuary.api.asksanctuary.openai;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;

public record AskSanctuaryClassification(
    AskSanctuaryIntent intent,
    AskSanctuaryModelUsage usage
) {
    public AskSanctuaryClassification(AskSanctuaryIntent intent) {
        this(intent, AskSanctuaryModelUsage.none());
    }
}
