package app.sanctuary.api.asksanctuary.openai;

public interface AskSanctuaryClassificationClient {
    default boolean isConfigured() {
        return true;
    }

    AskSanctuaryClassification classify(String message);
}
