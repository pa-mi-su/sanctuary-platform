package app.sanctuary.api.asksanctuary.openai;

public interface AskSanctuaryModelClient {
    AskSanctuaryModelOutput generate(AskSanctuaryModelRequest request);
}
