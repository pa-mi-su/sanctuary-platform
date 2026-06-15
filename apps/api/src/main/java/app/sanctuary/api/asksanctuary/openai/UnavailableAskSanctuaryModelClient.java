package app.sanctuary.api.asksanctuary.openai;

import org.springframework.stereotype.Component;

@Component
public class UnavailableAskSanctuaryModelClient implements AskSanctuaryModelClient {
    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public AskSanctuaryModelOutput generate(AskSanctuaryModelRequest request) {
        throw new AskSanctuaryModelException("Ask Sanctuary model client is not configured yet.");
    }
}
