package app.sanctuary.api.asksanctuary.openai;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;

@Component
@Primary
@ConditionalOnOpenAiApiKey
public class OpenAiAskSanctuaryModelClient implements AskSanctuaryModelClient, AskSanctuaryClassificationClient {

    private final AskSanctuaryOpenAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiAskSanctuaryModelClient(
        AskSanctuaryOpenAiProperties properties,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + properties.resolvedApiKey())
            .build();
    }

    @Override
    public AskSanctuaryClassification classify(String message) {
        try {
            Map<?, ?> response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(classificationRequestBody(message))
                .retrieve()
                .body(Map.class);

            String outputText = extractOutputText(response);
            if (outputText == null || outputText.isBlank()) {
                throw new AskSanctuaryModelException("OpenAI classification response did not include output text.");
            }

            ClassificationPayload payload = objectMapper.readValue(outputText, ClassificationPayload.class);
            AskSanctuaryIntent intent = AskSanctuaryIntent.valueOf(payload.intent());
            return new AskSanctuaryClassification(intent, extractUsage(response));
        } catch (IllegalArgumentException | NullPointerException | JsonProcessingException exception) {
            throw new AskSanctuaryModelException("OpenAI classification returned an invalid intent.", exception);
        } catch (RestClientException exception) {
            throw new AskSanctuaryModelException("OpenAI classification request failed.", exception);
        }
    }

    @Override
    public AskSanctuaryModelOutput generate(AskSanctuaryModelRequest request) {
        try {
            Map<?, ?> response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody(request))
                .retrieve()
                .body(Map.class);

            String outputText = extractOutputText(response);
            if (outputText == null || outputText.isBlank()) {
                throw new AskSanctuaryModelException("OpenAI response did not include output text.");
            }
            return new AskSanctuaryModelOutput(outputText, extractUsage(response));
        } catch (RestClientException exception) {
            throw new AskSanctuaryModelException("OpenAI request failed.", exception);
        }
    }

    private Map<String, Object> requestBody(AskSanctuaryModelRequest request) {
        return Map.of(
            "model", properties.resolvedModel(),
            "instructions", instructions(request.intent(), request.retry()),
            "input", request.message(),
            "max_output_tokens", properties.resolvedGenerationMaxOutputTokens(),
            "text", Map.of("format", schema())
        );
    }

    private Map<String, Object> classificationRequestBody(String message) {
        return Map.of(
            "model", properties.resolvedClassificationModel(),
            "instructions", classificationInstructions(),
            "input", message,
            "max_output_tokens", properties.resolvedClassificationMaxOutputTokens(),
            "text", Map.of("format", classificationSchema())
        );
    }

    private String classificationInstructions() {
        return """
            Classify one user message for Ask Sanctuary, a Catholic spiritual companion feature.
            Do not answer the user. Return only JSON matching the schema.

            Choose the best intent:
            - PRAYER_REQUEST: asks for prayer or wants to bring something to prayer.
            - LIFE_CONCERN: ordinary burdens, fatigue, stress, fear, sadness, anger, loneliness, uncertainty, or overwhelm.
            - GRIEF: death, mourning, loss, funeral, or bereavement.
            - FAMILY_HEALTH_CONCERN: family/member health, diagnosis, surgery, hospitalization, or illness.
            - RELATIONSHIP_CRISIS: betrayal, divorce, marriage/relationship crisis.
            - WORK_OR_DISCERNMENT: work, career, vocation, interview, decision, or discernment.
            - GRATITUDE: gratitude, thanksgiving, blessing, praise.
            - CATHOLIC_QUESTION: Catholic faith, sacraments, confession, Mass, saints, sin, or priest questions.
            - SCRIPTURE_HELP: asks about Bible, Scripture, verse, psalm, Gospel, or passage.
            - NOT_SPIRITUAL_OR_IRRELEVANT: jokes, spam, trivia, weather, markets, sports, or requests unrelated to prayer, Catholic faith, or serious life concerns.
            - VIOLENCE_RISK: intent or desire to harm another person.
            - SELF_HARM_RISK: intent or desire to self-harm or die.
            - EMERGENCY_OR_MEDICAL: urgent medical danger or emergency services need.
            - ABUSE_OR_DANGER: abuse, threats, domestic danger, or immediate unsafe situation.

            When in doubt between LIFE_CONCERN and NOT_SPIRITUAL_OR_IRRELEVANT, choose LIFE_CONCERN if the user names a real feeling, burden, need, or concern.
            """;
    }

    private String instructions(AskSanctuaryIntent intent, boolean retry) {
        String retryLine = retry
            ? "Previous output failed validation. Return only valid JSON matching the schema, with every required field populated."
            : "Return only valid JSON matching the schema.";

        return """
            You are Ask Sanctuary, a fenced Catholic companion feature.
            The backend already classified and guarded the user's message. Generate a Catholic companion payload for intent %s.
            Be Catholic, direct, practical, and human. Do not sound like therapy-speak or a greeting card.
            Do not replace a priest, doctor, therapist, lawyer, or emergency service.
            Do not tell people what major life decision to make. Do not say God caused suffering.
            Do not tell someone to forgive immediately in betrayal or abuse scenarios.
            Use cautious wording for sacramental or serious moral questions and suggest speaking with a priest when appropriate.
            Choose one Old Testament reference and one New Testament reference, plus one saint, one prayer, a reflection, and one concrete action.
            %s
            """.formatted(intent.name(), retryLine);
    }

    private Map<String, Object> schema() {
        Map<String, Object> scriptureReference = Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("book", "chapter", "verse"),
            "properties", Map.of(
                "book", Map.of("type", "string"),
                "chapter", Map.of("type", "string"),
                "verse", Map.of("type", "string")
            )
        );

        return Map.of(
            "type", "json_schema",
            "name", "ask_sanctuary_response",
            "strict", true,
            "schema", Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of(
                    "status",
                    "requiresAccount",
                    "requiresUpgrade",
                    "message",
                    "redirectAction",
                    "theme",
                    "oldTestament",
                    "newTestament",
                    "saint",
                    "prayer",
                    "reflection",
                    "action",
                    "intent",
                    "guardrail"
                ),
                "properties", Map.ofEntries(
                    Map.entry("status", Map.of("type", "string", "enum", List.of("OK"))),
                    Map.entry("requiresAccount", Map.of("type", "boolean")),
                    Map.entry("requiresUpgrade", Map.of("type", "boolean")),
                    Map.entry("message", nullableString()),
                    Map.entry("redirectAction", nullableString()),
                    Map.entry("theme", Map.of("type", "string")),
                    Map.entry("oldTestament", scriptureReference),
                    Map.entry("newTestament", scriptureReference),
                    Map.entry("saint", Map.of("type", "string")),
                    Map.entry("prayer", Map.of("type", "string")),
                    Map.entry("reflection", Map.of("type", "string")),
                    Map.entry("action", Map.of("type", "string")),
                    Map.entry("intent", Map.of("type", "string", "enum", intentNames())),
                    Map.entry("guardrail", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("type", "triggered"),
                        "properties", Map.of(
                            "type", Map.of("type", "string", "enum", List.of("NONE")),
                            "triggered", Map.of("type", "boolean")
                        )
                    ))
                )
            )
        );
    }

    private Map<String, Object> classificationSchema() {
        return Map.of(
            "type", "json_schema",
            "name", "ask_sanctuary_classification",
            "strict", true,
            "schema", Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("intent"),
                "properties", Map.of(
                    "intent", Map.of("type", "string", "enum", intentNames())
                )
            )
        );
    }

    private Map<String, Object> nullableString() {
        return Map.of("type", List.of("string", "null"));
    }

    private List<String> intentNames() {
        return java.util.Arrays.stream(AskSanctuaryIntent.values())
            .map(Enum::name)
            .toList();
    }

    private String extractOutputText(Map<?, ?> response) {
        if (response == null) {
            return null;
        }

        Object outputText = response.get("output_text");
        if (outputText instanceof String string) {
            return string;
        }

        Object output = response.get("output");
        if (!(output instanceof List<?> outputItems)) {
            return null;
        }

        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map<?, ?> outputMap)) {
                continue;
            }
            Object content = outputMap.get("content");
            if (!(content instanceof List<?> contentItems)) {
                continue;
            }
            for (Object contentItem : contentItems) {
                if (contentItem instanceof Map<?, ?> contentMap && contentMap.get("text") instanceof String text) {
                    return text;
                }
            }
        }
        return null;
    }

    private AskSanctuaryModelUsage extractUsage(Map<?, ?> response) {
        if (response == null) {
            return AskSanctuaryModelUsage.none();
        }

        String model = response.get("model") instanceof String value ? value : null;
        Object usage = response.get("usage");
        if (!(usage instanceof Map<?, ?> usageMap)) {
            return new AskSanctuaryModelUsage(model, null, null, null);
        }

        return new AskSanctuaryModelUsage(
            model,
            integerValue(usageMap.get("input_tokens")),
            integerValue(usageMap.get("output_tokens")),
            integerValue(usageMap.get("total_tokens"))
        );
    }

    private Integer integerValue(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private record ClassificationPayload(String intent) {
    }
}
