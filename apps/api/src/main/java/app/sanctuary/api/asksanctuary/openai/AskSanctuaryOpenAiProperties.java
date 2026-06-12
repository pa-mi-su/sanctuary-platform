package app.sanctuary.api.asksanctuary.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sanctuary.ask-sanctuary.openai")
public record AskSanctuaryOpenAiProperties(
    String apiKey,
    String model,
    String classificationModel,
    Integer generationMaxOutputTokens,
    Integer classificationMaxOutputTokens
) {
    public String resolvedApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    public String resolvedModel() {
        return model == null || model.isBlank() ? "gpt-4o-mini" : model;
    }

    public String resolvedClassificationModel() {
        return classificationModel == null || classificationModel.isBlank() ? "gpt-4.1-nano" : classificationModel;
    }

    public int resolvedGenerationMaxOutputTokens() {
        return generationMaxOutputTokens == null || generationMaxOutputTokens <= 0 ? 700 : generationMaxOutputTokens;
    }

    public int resolvedClassificationMaxOutputTokens() {
        return classificationMaxOutputTokens == null || classificationMaxOutputTokens <= 0 ? 80 : classificationMaxOutputTokens;
    }
}
