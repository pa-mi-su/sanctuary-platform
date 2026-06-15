package app.sanctuary.api.asksanctuary.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
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
import app.sanctuary.api.asksanctuary.repository.AskSanctuaryRecentContent;
import app.sanctuary.api.asksanctuary.repository.AskSanctuarySessionLog;
import app.sanctuary.api.asksanctuary.repository.UserFeatureConsentRepository;

@Service
public class AskSanctuaryService {
    private static final String FEATURE = "ASK_SANCTUARY";
    private static final String DISCLAIMER_VERSION = "v1";
    private static final String UNAVAILABLE_MESSAGE = "Sanctuary Companion is temporarily unavailable. Please try again later.";
    private static final int RECENT_CONTENT_LIMIT = 12;

    private final AskSanctuaryClassifier classifier;
    private final AskSanctuaryClassificationClient classificationClient;
    private final AskSanctuaryModelClient modelClient;
    private final AskSanctuaryInputValidator inputValidator;
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
        AskSanctuaryInputValidator inputValidator,
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
        this.inputValidator = inputValidator;
        this.validator = validator;
        this.repository = repository;
        this.usageService = usageService;
        this.consentRepository = consentRepository;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public AskSanctuaryStatusResponse status(UUID userId) {
        boolean available = isAvailable();
        return new AskSanctuaryStatusResponse(
            DISCLAIMER_VERSION,
            consentRepository.hasAccepted(userId, FEATURE, DISCLAIMER_VERSION),
            available,
            available ? null : UNAVAILABLE_MESSAGE
        );
    }

    public AskSanctuaryStatusResponse acceptDisclaimer(UUID userId) {
        consentRepository.accept(userId, FEATURE, DISCLAIMER_VERSION);
        boolean available = isAvailable();
        return new AskSanctuaryStatusResponse(
            DISCLAIMER_VERSION,
            true,
            available,
            available ? null : UNAVAILABLE_MESSAGE
        );
    }

    public AskSanctuaryResult answer(UUID userId, String message) {
        return answer(userId, message, null);
    }

    public AskSanctuaryResult answer(UUID userId, String message, String clientIpHash) {
        return answer(userId, message, clientIpHash, null);
    }

    public AskSanctuaryResult answer(UUID userId, String message, String clientIpHash, String locale) {
        AskSanctuaryIntent localIntent = classifier.classifyLocally(message).orElse(null);
        AskSanctuaryIntent initialIntent = localIntent == null ? AskSanctuaryIntent.LIFE_CONCERN : localIntent;
        String normalizedMessage = message == null ? "" : message.trim();
        String responseLocale = normalizeLocale(locale);
        boolean invalidFeelingInput = false;

        boolean available = isAvailable();

        if (available
            && initialIntent != AskSanctuaryIntent.SELF_HARM_RISK
            && initialIntent != AskSanctuaryIntent.EMERGENCY_OR_MEDICAL
            && initialIntent != AskSanctuaryIntent.ABUSE_OR_DANGER
            && localIntent != AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT
            && localIntent != AskSanctuaryIntent.VIOLENCE_RISK) {
            try {
                normalizedMessage = inputValidator.validate(message).normalizedMessage();
            } catch (IllegalArgumentException exception) {
                invalidFeelingInput = true;
                initialIntent = AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT;
            }
        }

        String inputHash = inputHash(normalizedMessage);

        AskSanctuaryResult result = !available
            ? serviceDisabled(initialIntent, responseLocale)
            : invalidFeelingInput
            ? invalidFeelingInput(responseLocale)
            : switch (initialIntent) {
            case SELF_HARM_RISK -> guarded(initialIntent, AskSanctuaryGuardrailType.SELF_HARM_RISK,
                guardrailMessage(responseLocale, "selfHarm"));
            case EMERGENCY_OR_MEDICAL -> guarded(initialIntent, AskSanctuaryGuardrailType.EMERGENCY_OR_MEDICAL,
                guardrailMessage(responseLocale, "medical"));
            case ABUSE_OR_DANGER -> guarded(initialIntent, AskSanctuaryGuardrailType.ABUSE_OR_DANGER,
                guardrailMessage(responseLocale, "abuse"));
            case VIOLENCE_RISK -> recordMisuseAndReturn(userId, guarded(initialIntent, AskSanctuaryGuardrailType.VIOLENCE_RISK,
                guardrailMessage(responseLocale, "violence")));
            default -> checked(normalizedMessage, responseLocale, localIntent, userId, clientIpHash, inputHash);
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

    private boolean isAvailable() {
        return properties.enabled() && modelClient.isConfigured() && classificationClient.isConfigured();
    }

    private AskSanctuaryResult checked(
        String message,
        String locale,
        AskSanctuaryIntent localIntent,
        UUID userId,
        String clientIpHash,
        String inputHash
    ) {
        if (usageService.isMisuseLocked(userId)) {
            return locked(localIntent == null ? AskSanctuaryIntent.LIFE_CONCERN : localIntent, locale);
        }

        if (usageService.isBurstLimited(userId)) {
            AskSanctuaryIntent intent = localIntent == null ? AskSanctuaryIntent.LIFE_CONCERN : localIntent;
            return misuseOrResult(userId, intent, AskSanctuaryGuardrailType.RATE_LIMIT, rateLimited(intent, locale), locale);
        }

        if (localIntent == AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT) {
            return misuseOrResult(userId, localIntent, AskSanctuaryGuardrailType.IRRELEVANT, redirect(localIntent, locale), locale);
        }

        AskSanctuaryQuotaDecision ipQuota = usageService.reserveDailyIpRequest(userId, clientIpHash);
        if (!ipQuota.allowed()) {
            AskSanctuaryIntent intent = localIntent == null ? AskSanctuaryIntent.LIFE_CONCERN : localIntent;
            return rateLimited(intent, locale);
        }

        var classification = localIntent == null
            ? classificationClient.classify(message)
            : new app.sanctuary.api.asksanctuary.openai.AskSanctuaryClassification(localIntent);
        AskSanctuaryIntent intent = localIntent == null
            ? classification.intent()
            : localIntent;

        if (intent == AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT) {
            return misuseOrResult(userId, intent, AskSanctuaryGuardrailType.IRRELEVANT, redirect(intent, locale), locale);
        }

        if (intent == AskSanctuaryIntent.VIOLENCE_RISK) {
            return recordMisuseAndReturn(userId, guarded(intent, AskSanctuaryGuardrailType.VIOLENCE_RISK,
                guardrailMessage(locale, "violence")));
        }

        AskSanctuaryQuotaDecision quota = usageService.reserveDailyCompanionRequest(userId);
        if (!quota.allowed()) {
            return limitReached(intent, quota.dailyLimit(), locale);
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

        List<AskSanctuaryRecentContent> recentContent = repository.findRecentContent(
            userId,
            OffsetDateTime.now().minusDays(30),
            RECENT_CONTENT_LIMIT
        );

        return normal(message, locale, intent, classification.usage(), recentContent);
    }

    private AskSanctuaryResult misuseOrResult(
        UUID userId,
        AskSanctuaryIntent intent,
        AskSanctuaryGuardrailType guardrailType,
        AskSanctuaryResult result,
        String locale
    ) {
        AskSanctuaryMisuseDecision decision = usageService.recordMisuse(userId, guardrailType);
        return decision.locked() ? locked(intent, locale) : result;
    }

    private AskSanctuaryResult recordMisuseAndReturn(UUID userId, AskSanctuaryResult result) {
        usageService.recordMisuse(userId, AskSanctuaryGuardrailType.VIOLENCE_RISK);
        return result;
    }

    private AskSanctuaryResult normal(
        String message,
        String locale,
        AskSanctuaryIntent intent,
        AskSanctuaryModelUsage classificationUsage,
        List<AskSanctuaryRecentContent> recentContent
    ) {
        try {
            AskSanctuaryModelOutput output = generateAndValidate(new AskSanctuaryModelRequest(
                message,
                locale,
                intent,
                false,
                variationGuidance(recentContent)
            ), recentContent);
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
                AskSanctuaryModelOutput retry = generateAndValidate(new AskSanctuaryModelRequest(
                    message,
                    locale,
                    intent,
                    true,
                    variationGuidance(recentContent)
                ), recentContent);
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
                AskSanctuaryResponse fallback = fallback(intent, locale);
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

    private AskSanctuaryModelOutput generateAndValidate(
        AskSanctuaryModelRequest request,
        List<AskSanctuaryRecentContent> recentContent
    ) {
        AskSanctuaryModelOutput output = modelClient.generate(request);
        AskSanctuaryResponse response = outputResponse(output);
        if (tooSimilarToRecent(response, recentContent)) {
            throw new AskSanctuaryModelException("Ask Sanctuary model repeated recent content.");
        }
        return output;
    }

    private boolean tooSimilarToRecent(AskSanctuaryResponse response, List<AskSanctuaryRecentContent> recentContent) {
        if (response == null || recentContent == null || recentContent.isEmpty()) {
            return false;
        }

        String oldTestament = referenceText(response.oldTestament());
        String newTestament = referenceText(response.newTestament());
        for (AskSanctuaryRecentContent recent : recentContent) {
            if (sameText(response.theme(), recent.theme())) {
                return true;
            }
            if (sameText(oldTestament, recent.oldTestamentReference())
                || sameText(newTestament, recent.newTestamentReference())) {
                return true;
            }
            if (sameText(response.saint(), recent.saint()) && sameText(response.prayer(), recent.prayer())) {
                return true;
            }
        }

        return false;
    }

    private boolean sameText(String left, String right) {
        String normalizedLeft = normalizeComparable(left);
        String normalizedRight = normalizeComparable(right);
        return !normalizedLeft.isBlank() && normalizedLeft.equals(normalizedRight);
    }

    private String normalizeComparable(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String referenceText(ScriptureReferenceDto reference) {
        if (reference == null) {
            return null;
        }
        return "%s %s:%s".formatted(reference.book(), reference.chapter(), reference.verse());
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

    private AskSanctuaryResult redirect(AskSanctuaryIntent intent, String locale) {
        AskSanctuaryResponse response = new AskSanctuaryResponse(
            "REDIRECT",
            false,
            false,
            promptHelp(locale),
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

    private AskSanctuaryResult invalidFeelingInput(String locale) {
        return redirect(AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT, locale);
    }

    private AskSanctuaryResult limitReached(AskSanctuaryIntent intent, int dailyLimit, String locale) {
        AskSanctuaryResponse response = AskSanctuaryResponse.limitReached(dailyLimit, locale);
        return new AskSanctuaryResult(AskSanctuaryStatus.LIMIT_REACHED, intent, AskSanctuaryGuardrailType.DAILY_LIMIT, true, response);
    }

    private AskSanctuaryResult rateLimited(AskSanctuaryIntent intent, String locale) {
        AskSanctuaryResponse response = AskSanctuaryResponse.rateLimited(locale);
        return new AskSanctuaryResult(AskSanctuaryStatus.RATE_LIMITED, intent, AskSanctuaryGuardrailType.RATE_LIMIT, true, response);
    }

    private AskSanctuaryResult serviceDisabled(AskSanctuaryIntent intent, String locale) {
        AskSanctuaryResponse response = AskSanctuaryResponse.serviceDisabled(locale);
        return new AskSanctuaryResult(AskSanctuaryStatus.SERVICE_DISABLED, intent, AskSanctuaryGuardrailType.SERVICE_DISABLED, true, response);
    }

    private AskSanctuaryResult locked(AskSanctuaryIntent intent, String locale) {
        AskSanctuaryResponse response = AskSanctuaryResponse.locked(locale);
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

    private AskSanctuaryResponse fallback(AskSanctuaryIntent intent, String locale) {
        return new AskSanctuaryResponse(
            "FALLBACK",
            false,
            false,
            fallbackMessage(locale),
            null,
            fallbackTheme(locale),
            new ScriptureReferenceDto("Psalms", "23", "1"),
            new ScriptureReferenceDto("Matthew", "11", "28"),
            "St. Joseph",
            fallbackPrayer(locale),
            fallbackReflection(locale),
            fallbackAction(locale),
            intent.name(),
            new AskSanctuaryGuardrailDto(AskSanctuaryGuardrailType.MODEL_FALLBACK.name(), true)
        );
    }

    private String promptHelp(String locale) {
        return switch (locale) {
            case "es" -> "Compañero Sanctuary funciona mejor con una sola palabra sencilla sobre cómo te sientes.";
            case "pl" -> "Towarzysz Sanctuary działa najlepiej z jednym prostym słowem o tym, co czujesz.";
            default -> "Sanctuary Companion works best with one simple feeling word.";
        };
    }

    private String variationGuidance(List<AskSanctuaryRecentContent> recentContent) {
        if (recentContent == null || recentContent.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder("Recent content for this user from the last 30 days. Avoid repeating these exact ingredients when possible:\n");
        for (AskSanctuaryRecentContent content : recentContent) {
            builder.append("- Theme: ")
                .append(valueOrDash(content.theme()))
                .append("; Old Testament: ")
                .append(valueOrDash(content.oldTestamentReference()))
                .append("; New Testament: ")
                .append(valueOrDash(content.newTestamentReference()))
                .append("; Saint: ")
                .append(valueOrDash(content.saint()))
                .append("; Prayer: ")
                .append(valueOrDash(content.prayer()))
                .append('\n');
        }
        return builder.toString();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String guardrailMessage(String locale, String key) {
        return switch (locale) {
            case "es" -> switch (key) {
                case "selfHarm" -> "Tu vida importa. Si podrías hacerte daño o estás en peligro inmediato, contacta a emergencias ahora o acércate a alguien de confianza que pueda quedarse contigo.";
                case "medical" -> "Esto suena urgente. Contacta de inmediato a emergencias o a un profesional médico si hay peligro médico inmediato.";
                case "abuse" -> "Si estás en peligro, muévete hacia un lugar seguro y contacta a emergencias o a una persona de confianza. Sanctuary puede orar contigo, pero la seguridad inmediata va primero.";
                case "violence" -> "Crea distancia ahora. No hagas daño a nadie. Si hay peligro inmediato, contacta a emergencias o a una persona de confianza cercana ahora mismo.";
                default -> promptHelp(locale);
            };
            case "pl" -> switch (key) {
                case "selfHarm" -> "Twoje życie ma znaczenie. Jeśli możesz zrobić sobie krzywdę albo jesteś w bezpośrednim niebezpieczeństwie, skontaktuj się teraz ze służbami ratunkowymi albo z kimś zaufanym, kto może być przy Tobie.";
                case "medical" -> "To brzmi pilnie. Jeśli istnieje bezpośrednie zagrożenie medyczne, natychmiast skontaktuj się ze służbami ratunkowymi lub lekarzem.";
                case "abuse" -> "Jeśli jesteś w niebezpieczeństwie, przejdź w bezpieczne miejsce i skontaktuj się ze służbami ratunkowymi lub zaufaną osobą. Sanctuary może modlić się z Tobą, ale natychmiastowe bezpieczeństwo jest pierwsze.";
                case "violence" -> "Stwórz dystans teraz. Nie krzywdź nikogo. Jeśli istnieje bezpośrednie niebezpieczeństwo, skontaktuj się teraz ze służbami ratunkowymi albo z zaufaną osobą w pobliżu.";
                default -> promptHelp(locale);
            };
            default -> switch (key) {
                case "selfHarm" -> "You matter. If you might hurt yourself or are in immediate danger, contact emergency services now or reach out to someone you trust who can stay with you.";
                case "medical" -> "This sounds urgent. Please contact emergency services or a medical professional right away if there is immediate medical danger.";
                case "abuse" -> "If you are in danger, move toward safety and contact emergency services or a trusted person. Sanctuary can pray with you, but immediate safety comes first.";
                case "violence" -> "Create distance now. Do not harm anyone. If there is immediate danger, contact emergency services or a trusted person near you right away.";
                default -> promptHelp(locale);
            };
        };
    }

    private String fallbackMessage(String locale) {
        return switch (locale) {
            case "es" -> "Compañero Sanctuary no pudo preparar una respuesta completa en este momento.";
            case "pl" -> "Towarzysz Sanctuary nie mógł teraz przygotować pełnej odpowiedzi.";
            default -> "Sanctuary Companion could not prepare a full response right now.";
        };
    }

    private String fallbackTheme(String locale) {
        return switch (locale) {
            case "es" -> "Lleva esto a la oración";
            case "pl" -> "Przynieś to do modlitwy";
            default -> "Bring this to prayer";
        };
    }

    private String fallbackPrayer(String locale) {
        return switch (locale) {
            case "es" -> "Padre nuestro";
            case "pl" -> "Ojcze nasz";
            default -> "Our Father";
        };
    }

    private String fallbackReflection(String locale) {
        return switch (locale) {
            case "es" -> "Haz una pausa, nombra lo que llevas y pide a Dios la gracia para dar el próximo paso fiel.";
            case "pl" -> "Zatrzymaj się, nazwij to, co niesiesz, i poproś Boga o łaskę następnego wiernego kroku.";
            default -> "Pause, name what you are carrying, and ask God for the grace to take the next faithful step.";
        };
    }

    private String fallbackAction(String locale) {
        return switch (locale) {
            case "es" -> "Toma un minuto en silencio y reza despacio el Padre nuestro.";
            case "pl" -> "Weź jedną cichą minutę i powoli odmów Ojcze nasz.";
            default -> "Take one quiet minute and pray the Our Father slowly.";
        };
    }

    private String normalizeLocale(String locale) {
        if (locale == null) {
            return "en";
        }
        return switch (locale.trim().toLowerCase()) {
            case "es", "es-es", "es_419", "es-419" -> "es";
            case "pl", "pl-pl" -> "pl";
            default -> "en";
        };
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
