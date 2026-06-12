package app.sanctuary.api.asksanctuary.openai;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

class OpenAiApiKeyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String apiKey = context.getEnvironment().getProperty("sanctuary.ask-sanctuary.openai.api-key");
        return apiKey != null && !apiKey.isBlank();
    }
}
