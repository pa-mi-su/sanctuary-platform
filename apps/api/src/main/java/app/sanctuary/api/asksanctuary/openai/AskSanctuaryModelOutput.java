package app.sanctuary.api.asksanctuary.openai;

public record AskSanctuaryModelOutput(
    String text,
    AskSanctuaryModelUsage usage
) {
}
