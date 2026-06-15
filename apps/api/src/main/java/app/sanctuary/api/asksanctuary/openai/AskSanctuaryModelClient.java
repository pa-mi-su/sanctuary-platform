package app.sanctuary.api.asksanctuary.openai;

public interface AskSanctuaryModelClient {
    default boolean isConfigured() {
        return true;
    }

    AskSanctuaryModelOutput generate(AskSanctuaryModelRequest request);
}
