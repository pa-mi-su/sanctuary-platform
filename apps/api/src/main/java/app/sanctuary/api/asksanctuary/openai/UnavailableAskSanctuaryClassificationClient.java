package app.sanctuary.api.asksanctuary.openai;

import org.springframework.stereotype.Component;

@Component
public class UnavailableAskSanctuaryClassificationClient implements AskSanctuaryClassificationClient {
    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public AskSanctuaryClassification classify(String message) {
        throw new AskSanctuaryModelException("Ask Sanctuary classification client is not configured yet.");
    }
}
