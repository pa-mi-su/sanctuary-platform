package app.sanctuary.api.asksanctuary.openai;

public record AskSanctuaryModelUsage(
    String model,
    Integer inputTokens,
    Integer outputTokens,
    Integer totalTokens
) {
    public static AskSanctuaryModelUsage none() {
        return new AskSanctuaryModelUsage(null, null, null, null);
    }
}
