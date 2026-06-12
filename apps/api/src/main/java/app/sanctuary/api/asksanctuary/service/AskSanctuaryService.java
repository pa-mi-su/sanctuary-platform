package app.sanctuary.api.asksanctuary.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import app.sanctuary.api.asksanctuary.limits.AskSanctuaryMisuseDecision;
import app.sanctuary.api.asksanctuary.limits.AskSanctuaryQuotaDecision;
import app.sanctuary.api.asksanctuary.limits.AskSanctuaryUsageService;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryGuardrailDto;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.dto.AskSanctuaryStatusResponse;
import app.sanctuary.api.asksanctuary.dto.ScriptureReferenceDto;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryResult;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryStatus;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelClient;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelException;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelOutput;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelRequest;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryClassificationClient;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelUsage;
import app.sanctuary.api.asksanctuary.repository.AskSanctuaryRepository;
import app.sanctuary.api.asksanctuary.repository.AskSanctuarySessionLog;
import app.sanctuary.api.asksanctuary.repository.UserFeatureConsentRepository;

@Service
public class AskSanctuaryService {
    private static final String FEATURE = "ASK_SANCTUARY";
    private static final String DISCLAIMER_VERSION = "v1";
    private static final String UNAVAILABLE_MESSAGE = "Ask Sanctuary is temporarily unavailable. Please try again later.";

    private final AskSanctuaryClassifier classifier;
    private final AskSanctuaryClassificationClient classificationClient;
    private final AskSanctuaryModelClient modelClient;
    private final AskSanctuaryPayloadValidator validator;
    private final AskSanctuaryRepository repository;
    private final AskSanctuaryUsageService usageService;
    private final UserFeatureConsentRepository consentRepository;
    private final ObjectMapper objectMapper;
    private final AskSanctuaryProperties properties;

    public AskSanctuaryService(
        AskSanctuaryClassifier classifier,
        AskSanctuaryClassificationClient classificationClient,
        AskSanctuaryModelClient modelClient,
        AskSanctuaryPayloadValidator validator,
        AskSanctuaryRepository repository,
        AskSanctuaryUsageService usageService,
        UserFeatureConsentRepository consentRepository,
        ObjectMapper objectMapper,
        AskSanctuaryProperties properties
    ) {
        this.classifier = classifier;
        this.classificationClient = classificationClient;
        this.modelClient = modelClient;
        this.validator = validator;
        this.repository = repository;
        this.usageService = usageService;
        this.consentRepository = consentRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public AskSanctuaryStatusResponse status(UUID userId) {
        return new AskSanctuaryStatusResponse(
            DISCLAIMER_VERSION,
            consentRepository.hasAccepted(userId, FEATURE, DISCLAIMER_VERSION),
            properties.enabled(),
            properties.enabled() ? null : UNAVAILABLE_MESSAGE
        );
    }

    public AskSanctuaryStatusResponse acceptDisclaimer(UUID userId) {
        consentRepository.accept(userId, FEATURE, DISCLAIMER_VERSION);
        return new AskSanctuaryStatusResponse(
            DISCLAIMER_VERSION,
            true,
            properties.enabled(),
            properties.enabled() ? null : UNAVAILABLE_MESSAGE
        );
    }

    public AskSanctuaryResult answer(UUID userId, String message) {
        return answer(userId, message, null);
    }

    public AskSanctuaryResult answer(UUID userId, String message, String clientIpHash) {
        String inputHash = inputHash(message);
        AskSanctuaryIntent localIntent = classifier.classifyLocally(message).orElse(null);
        AskSanctuaryIntent initialIntent = localIntent == null ? AskSanctuaryIntent.LIFE_CONCERN : localIntent;

        AskSanctuaryResult result = !properties.enabled()
            ? serviceDisabled(initialIntent)
            : switch (initialIntent) {
            case SELF_HARM_RISK -> guarded(initialIntent, AskSanctuaryGuardrailType.SELF_HARM_RISK,
                "You matter. If you might hurt yourself or are in immediate danger, contact emergency services now or reach out to someone you trust who can stay with you.");
            case EMERGENCY_OR_MEDICAL -> guarded(initialIntent, AskSanctuaryGuardrailType.EMERGENCY_OR_MEDICAL,
                "This sounds urgent. Please contact emergency services or a medical professional right away if there is immediate medical danger.");
            case ABUSE_OR_DANGER -> guarded(initialIntent, AskSanctuaryGuardrailType.ABUSE_OR_DANGER,
                "If you are in danger, move toward safety and contact emergency services or a trusted person. Sanctuary can pray with you, but immediate safety comes first.");
            default -> checked(message, localIntent, userId, clientIpHash, inputHash);
        };

        repository.save(new AskSanctuarySessionLog(
            userId,
            message,
            inputHash,
            result.intent(),
            result.guardrailType(),
            result.guardrailTriggered(),
            result.status(),
            result.response(),
            toJson(result.response()),
            result.reusedResponse(),
            result.classificationUsage(),
            result.generationUsage()
        ));

        return result;
    }

    private AskSanctuaryResult checked(
        String message,
        AskSanctuaryIntent localIntent,
        UUID userId,
        String clientIpHash,
        String inputHash
    ) {
        if (usageService.isMisuseLocked(userId)) {
            return locked(localIntent == null ? AskSanctuaryIntent.LIFE_CONCERN : localIntent);
        }

        if (usageService.isBurstLimited(userId)) {
            AskSanctuaryIntent intent = localIntent == null ? AskSanctuaryIntent.LIFE_CONCERN : localIntent;
            return misuseOrResult(userId, intent, AskSanctuaryGuardrailType.RATE_LIMIT, rateLimited(intent));
        }

        if (localIntent == AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT) {
            return misuseOrResult(userId, localIntent, AskSanctuaryGuardrailType.IRRELEVANT, redirect(localIntent));
        }

        if (localIntent == AskSanctuaryIntent.VIOLENCE_RISK) {
            return misuseOrResult(userId, localIntent, AskSanctuaryGuardrailType.VIOLENCE_RISK, guarded(localIntent, AskSanctuaryGuardrailType.VIOLENCE_RISK,
                "Create distance now. Do not harm anyone. If there is immediate danger, contact emergency services or a trusted person near you right away."));
        }

        if (properties.cache().enabled()) {
            OffsetDateTime cutoff = OffsetDateTime.now().minus(properties.cache().window());
            var cached = repository.findRecentReusableResponse(userId, inputHash, cutoff);
            if (cached.isPresent()) {
                var value = cached.get();
                return new AskSanctuaryResult(
                    value.status(),
                    value.intent(),
                    value.guardrailType(),
                    value.guardrailTriggered(),
                    value.response(),
                    true,
                    AskSanctuaryModelUsage.none(),
                    AskSanctuaryModelUsage.none()
                );
            }
        }

        AskSanctuaryQuotaDecision ipQuota = usageService.reserveDailyIpRequest(userId, clientIpHash);
        if (!ipQuota.allowed()) {
            AskSanctuaryIntent intent = localIntent == null ? AskSanctuaryIntent.LIFE_CONCERN : localIntent;
            return rateLimited(intent);
        }

        var classification = localIntent == null
            ? classificationClient.classify(message)
            : new app.sanctuary.api.asksanctuary.openai.AskSanctuaryClassification(localIntent);
        AskSanctuaryIntent intent = localIntent == null
            ? classification.intent()
            : localIntent;

        if (intent == AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT) {
            return misuseOrResult(userId, intent, AskSanctuaryGuardrailType.IRRELEVANT, redirect(intent));
        }

        if (intent == AskSanctuaryIntent.VIOLENCE_RISK) {
            return misuseOrResult(userId, intent, AskSanctuaryGuardrailType.VIOLENCE_RISK, guarded(intent, AskSanctuaryGuardrailType.VIOLENCE_RISK,
                "Create distance now. Do not harm anyone. If there is immediate danger, contact emergency services or a trusted person near you right away."));
        }

        AskSanctuaryQuotaDecision quota = usageService.reserveDailyCompanionRequest(userId);
        if (!quota.allowed()) {
            return limitReached(intent, quota.dailyLimit());
        }

        return normal(message, intent, classification.usage());
    }

    private AskSanctuaryResult misuseOrResult(
        UUID userId,
        AskSanctuaryIntent intent,
        AskSanctuaryGuardrailType guardrailType,
        AskSanctuaryResult result
    ) {
        AskSanctuaryMisuseDecision decision = usageService.recordMisuse(userId, guardrailType);
        return decision.locked() ? locked(intent) : result;
    }

    private AskSanctuaryResult normal(String message, AskSanctuaryIntent intent, AskSanctuaryModelUsage classificationUsage) {
        try {
            AskSanctuaryModelOutput output = generateAndValidate(new AskSanctuaryModelRequest(message, intent, false));
            return new AskSanctuaryResult(
                AskSanctuaryStatus.OK,
                intent,
                AskSanctuaryGuardrailType.NONE,
                false,
                outputResponse(output),
                false,
                classificationUsage,
                output.usage()
            );
        } catch (RuntimeException firstFailure) {
            try {
                AskSanctuaryModelOutput retry = generateAndValidate(new AskSanctuaryModelRequest(message, intent, true));
                return new AskSanctuaryResult(
                    AskSanctuaryStatus.OK,
                    intent,
                    AskSanctuaryGuardrailType.NONE,
                    false,
                    outputResponse(retry),
                    false,
                    classificationUsage,
                    retry.usage()
                );
            } catch (RuntimeException secondFailure) {
                AskSanctuaryResponse fallback = fallback(intent);
                return new AskSanctuaryResult(
                    AskSanctuaryStatus.FALLBACK,
                    intent,
                    AskSanctuaryGuardrailType.MODEL_FALLBACK,
                    true,
                    fallback,
                    false,
                    classificationUsage,
                    AskSanctuaryModelUsage.none()
                );
            }
        }
    }

    private AskSanctuaryModelOutput generateAndValidate(AskSanctuaryModelRequest request) {
        AskSanctuaryModelOutput output = modelClient.generate(request);
        outputResponse(output);
        return output;
    }

    private AskSanctuaryResponse outputResponse(AskSanctuaryModelOutput output) {
        try {
            AskSanctuaryResponse response = objectMapper.readValue(output.text(), AskSanctuaryResponse.class);
            validator.validate(response);
            return response;
        } catch (JsonProcessingException exception) {
            throw new AskSanctuaryModelException("Ask Sanctuary model returned invalid JSON.");
        }
    }

    private AskSanctuaryResult redirect(AskSanctuaryIntent intent) {
        AskSanctuaryResponse response = new AskSanctuaryResponse(
            "REDIRECT",
            false,
            false,
            "Sanctuary is designed for prayer, Catholic faith, spiritual support, and serious life concerns. Tell me what you’re carrying or what you’d like prayer for.",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            intent.name(),
            new AskSanctuaryGuardrailDto(AskSanctuaryGuardrailType.IRRELEVANT.name(), true)
        );
        return new AskSanctuaryResult(AskSanctuaryStatus.REDIRECT, intent, AskSanctuaryGuardrailType.IRRELEVANT, true, response);
    }

    private AskSanctuaryResult limitReached(AskSanctuaryIntent intent, int dailyLimit) {
        AskSanctuaryResponse response = AskSanctuaryResponse.limitReached(dailyLimit);
        return new AskSanctuaryResult(AskSanctuaryStatus.LIMIT_REACHED, intent, AskSanctuaryGuardrailType.DAILY_LIMIT, true, response);
    }

    private AskSanctuaryResult rateLimited(AskSanctuaryIntent intent) {
        AskSanctuaryResponse response = AskSanctuaryResponse.rateLimited();
        return new AskSanctuaryResult(AskSanctuaryStatus.RATE_LIMITED, intent, AskSanctuaryGuardrailType.RATE_LIMIT, true, response);
    }

    private AskSanctuaryResult serviceDisabled(AskSanctuaryIntent intent) {
        AskSanctuaryResponse response = AskSanctuaryResponse.serviceDisabled();
        return new AskSanctuaryResult(AskSanctuaryStatus.SERVICE_DISABLED, intent, AskSanctuaryGuardrailType.SERVICE_DISABLED, true, response);
    }

    private AskSanctuaryResult locked(AskSanctuaryIntent intent) {
        AskSanctuaryResponse response = AskSanctuaryResponse.locked();
        return new AskSanctuaryResult(AskSanctuaryStatus.LOCKED, intent, AskSanctuaryGuardrailType.MISUSE_LOCK, true, response);
    }

    private AskSanctuaryResult guarded(AskSanctuaryIntent intent, AskSanctuaryGuardrailType guardrailType, String message) {
        AskSanctuaryResponse response = new AskSanctuaryResponse(
            "GUARDED",
            false,
            false,
            message,
            null,
            "Immediate safety comes first",
            null,
            null,
            null,
            null,
            message,
            "Reach out to emergency services or a trusted person if there is immediate danger.",
            intent.name(),
            new AskSanctuaryGuardrailDto(guardrailType.name(), true)
        );
        return new AskSanctuaryResult(AskSanctuaryStatus.GUARDED, intent, guardrailType, true, response);
    }

    private AskSanctuaryResponse fallback(AskSanctuaryIntent intent) {
        return new AskSanctuaryResponse(
            "FALLBACK",
            false,
            false,
            "Ask Sanctuary could not prepare a full response right now.",
            null,
            "Bring this to prayer",
            new ScriptureReferenceDto("Psalms", "23", "1"),
            new ScriptureReferenceDto("Matthew", "11", "28"),
            "St. Joseph",
            "Our Father",
            "Pause, name what you are carrying, and ask God for the grace to take the next faithful step.",
            "Take one quiet minute and pray the Our Father slowly.",
            intent.name(),
            new AskSanctuaryGuardrailDto(AskSanctuaryGuardrailType.MODEL_FALLBACK.name(), true)
        );
    }

    private String toJson(AskSanctuaryResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Ask Sanctuary response must be serializable.", exception);
        }
    }

    private String inputHash(String message) {
        String normalized = message == null ? "" : message.trim().replaceAll("\\s+", " ").toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available.", exception);
        }
    }
}
