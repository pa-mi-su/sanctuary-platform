package app.sanctuary.api.asksanctuary.openai;

public interface AskSanctuaryClassificationClient {
    AskSanctuaryClassification classify(String message);
}
