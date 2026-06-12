package app.sanctuary.api.asksanctuary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryGuardrailType;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryResult;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryStatus;
import app.sanctuary.api.asksanctuary.limits.AskSanctuaryMisuseDecision;
import app.sanctuary.api.asksanctuary.limits.AskSanctuaryQuotaDecision;
import app.sanctuary.api.asksanctuary.limits.AskSanctuaryUsageService;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryClassification;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryClassificationClient;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelClient;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelOutput;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelRequest;
import app.sanctuary.api.asksanctuary.openai.AskSanctuaryModelUsage;
import app.sanctuary.api.asksanctuary.repository.AskSanctuaryCachedResponse;
import app.sanctuary.api.asksanctuary.repository.AskSanctuaryRepository;
import app.sanctuary.api.asksanctuary.repository.AskSanctuarySessionLog;
import app.sanctuary.api.asksanctuary.repository.UserFeatureConsentRepository;

@ExtendWith(MockitoExtension.class)
class AskSanctuaryServiceTest {

    @Mock
    private AskSanctuaryModelClient modelClient;

    @Mock
    private AskSanctuaryClassificationClient classificationClient;

    @Mock
    private AskSanctuaryRepository repository;

    @Mock
    private AskSanctuaryUsageService usageService;

    @Mock
    private UserFeatureConsentRepository consentRepository;

    @Test
    void statusReturnsServerStoredDisclaimerAcceptance() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(consentRepository.hasAccepted(userId, "ASK_SANCTUARY", "v1")).thenReturn(true);

        var status = service.status(userId);

        assertThat(status.disclaimerVersion()).isEqualTo("v1");
        assertThat(status.disclaimerAccepted()).isTrue();
        assertThat(status.available()).isTrue();
        assertThat(status.unavailableMessage()).isNull();
    }

    @Test
    void statusReturnsUnavailableWhenKillSwitchIsOff() {
        AskSanctuaryService service = service(false);
        UUID userId = UUID.randomUUID();
        when(consentRepository.hasAccepted(userId, "ASK_SANCTUARY", "v1")).thenReturn(true);

        var status = service.status(userId);

        assertThat(status.disclaimerVersion()).isEqualTo("v1");
        assertThat(status.disclaimerAccepted()).isTrue();
        assertThat(status.available()).isFalse();
        assertThat(status.unavailableMessage()).isEqualTo("Ask Sanctuary is temporarily unavailable. Please try again later.");
    }

    @Test
    void acceptDisclaimerPersistsVersionedConsent() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();

        var status = service.acceptDisclaimer(userId);

        assertThat(status.disclaimerVersion()).isEqualTo("v1");
        assertThat(status.disclaimerAccepted()).isTrue();
        assertThat(status.available()).isTrue();
        assertThat(status.unavailableMessage()).isNull();
        verify(consentRepository).accept(userId, "ASK_SANCTUARY", "v1");
    }

    @Test
    void guardrailResponseDoesNotCallModelAndPersistsHistory() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(usageService.recordMisuse(userId, AskSanctuaryGuardrailType.VIOLENCE_RISK))
            .thenReturn(new AskSanctuaryMisuseDecision(false, 1));

        AskSanctuaryResult result = service.answer(userId, "I want to murder my friend.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.GUARDED);
        assertThat(result.intent()).isEqualTo(AskSanctuaryIntent.VIOLENCE_RISK);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.VIOLENCE_RISK);
        assertThat(result.response().guardrail().triggered()).isTrue();
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
        verify(repository).save(any(AskSanctuarySessionLog.class));
    }

    @Test
    void irrelevantMessageRedirectsWithoutCallingModel() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(usageService.recordMisuse(userId, AskSanctuaryGuardrailType.IRRELEVANT))
            .thenReturn(new AskSanctuaryMisuseDecision(false, 1));

        AskSanctuaryResult result = service.answer(userId, "I just shit my pants.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.REDIRECT);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.IRRELEVANT);
        assertThat(result.response().message()).contains("Sanctuary is designed for prayer");
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void normalMessageClassifiesThenCallsModelAndPersistsValidatedPayload() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("I have a job interview tomorrow."))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.WORK_OR_DISCERNMENT));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.allowed(3, 1));
        when(modelClient.generate(any())).thenReturn(modelOutput("WORK_OR_DISCERNMENT"));

        AskSanctuaryResult result = service.answer(userId, "I have a job interview tomorrow.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.OK);
        assertThat(result.intent()).isEqualTo(AskSanctuaryIntent.WORK_OR_DISCERNMENT);
        assertThat(result.response().theme()).isEqualTo("Courage for today");
        verify(classificationClient).classify("I have a job interview tomorrow.");
        verify(modelClient).generate(any(AskSanctuaryModelRequest.class));

        ArgumentCaptor<AskSanctuarySessionLog> logCaptor = ArgumentCaptor.forClass(AskSanctuarySessionLog.class);
        verify(repository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().responsePayload()).contains("\"theme\":\"Courage for today\"");
        assertThat(logCaptor.getValue().generationUsage().model()).isEqualTo("gpt-test");
    }

    @Test
    void invalidModelPayloadRetriesOnceThenFallsBack() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("I am worried."))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.LIFE_CONCERN));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.allowed(3, 1));
        when(modelClient.generate(any()))
            .thenReturn(new AskSanctuaryModelOutput("{\"status\":\"OK\"}", AskSanctuaryModelUsage.none()))
            .thenReturn(new AskSanctuaryModelOutput("{\"status\":\"OK\"}", AskSanctuaryModelUsage.none()));

        AskSanctuaryResult result = service.answer(userId, "I am worried.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.FALLBACK);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.MODEL_FALLBACK);
        assertThat(result.response().oldTestament().book()).isEqualTo("Psalms");
        verify(modelClient, times(2)).generate(any());
    }

    @Test
    void dailyLimitReachedDoesNotCallModelAndPersistsLimitResponse() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("I am worried."))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.LIFE_CONCERN));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.denied(3));

        AskSanctuaryResult result = service.answer(userId, "I am worried.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.LIMIT_REACHED);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.DAILY_LIMIT);
        assertThat(result.response().requiresUpgrade()).isTrue();
        assertThat(result.response().redirectAction()).isEqualTo("UPGRADE");
        verify(classificationClient).classify("I am worried.");
        verify(modelClient, never()).generate(any());
        verify(repository).save(any(AskSanctuarySessionLog.class));
    }

    @Test
    void burstLimitDoesNotCallModelOrReserveDailyQuota() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(true);
        when(usageService.recordMisuse(userId, AskSanctuaryGuardrailType.RATE_LIMIT))
            .thenReturn(new AskSanctuaryMisuseDecision(false, 1));

        AskSanctuaryResult result = service.answer(userId, "I am worried.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.RATE_LIMITED);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.RATE_LIMIT);
        assertThat(result.response().redirectAction()).isEqualTo("TRY_LATER");
        verify(classificationClient, never()).classify(any());
        verify(usageService, never()).reserveDailyCompanionRequest(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void activeMisuseLockBlocksNormalRequestBeforeQuotaAndModel() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(true);

        AskSanctuaryResult result = service.answer(userId, "I am worried.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.LOCKED);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.MISUSE_LOCK);
        assertThat(result.response().redirectAction()).isEqualTo("TRY_LATER");
        verify(classificationClient, never()).classify(any());
        verify(usageService, never()).isBurstLimited(any());
        verify(usageService, never()).reserveDailyCompanionRequest(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void thirdMisuseLocksAccountAndDoesNotCallModel() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(usageService.recordMisuse(userId, AskSanctuaryGuardrailType.IRRELEVANT))
            .thenReturn(new AskSanctuaryMisuseDecision(true, 3));

        AskSanctuaryResult result = service.answer(userId, "I just shit my pants.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.LOCKED);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.MISUSE_LOCK);
        assertThat(result.response().message()).contains("repeated misuse");
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
        verify(repository).save(any(AskSanctuarySessionLog.class));
    }

    @Test
    void trueCrisisSafetyResponseBypassesMisuseLock() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();

        AskSanctuaryResult result = service.answer(userId, "I want to kill myself.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.GUARDED);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.SELF_HARM_RISK);
        verify(classificationClient, never()).classify(any());
        verify(usageService, never()).isMisuseLocked(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void ordinaryFeelingUsesModelClassificationInsteadOfKeywordList() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("I feel tired today."))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.LIFE_CONCERN));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.allowed(3, 1));
        when(modelClient.generate(any())).thenReturn(modelOutput("LIFE_CONCERN"));

        AskSanctuaryResult result = service.answer(userId, "I feel tired today.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.OK);
        assertThat(result.intent()).isEqualTo(AskSanctuaryIntent.LIFE_CONCERN);
        verify(classificationClient).classify("I feel tired today.");
        verify(modelClient).generate(any(AskSanctuaryModelRequest.class));
    }

    @Test
    void modelClassifiedIrrelevantMessageRedirectsWithoutGeneratingAnswer() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("Tell me the football score."))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.recordMisuse(userId, AskSanctuaryGuardrailType.IRRELEVANT))
            .thenReturn(new AskSanctuaryMisuseDecision(false, 1));

        AskSanctuaryResult result = service.answer(userId, "Tell me the football score.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.REDIRECT);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.IRRELEVANT);
        verify(classificationClient).classify("Tell me the football score.");
        verify(modelClient, never()).generate(any());
    }

    @Test
    void cachedResponseAvoidsClassificationQuotaAndGeneration() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(repository.findRecentReusableResponse(eq(userId), any(), any()))
            .thenReturn(java.util.Optional.of(new AskSanctuaryCachedResponse(
                AskSanctuaryIntent.LIFE_CONCERN,
                AskSanctuaryGuardrailType.NONE,
                false,
                AskSanctuaryStatus.OK,
                response("LIFE_CONCERN")
            )));

        AskSanctuaryResult result = service.answer(userId, "I feel tired today.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.OK);
        assertThat(result.reusedResponse()).isTrue();
        verify(classificationClient, never()).classify(any());
        verify(usageService, never()).reserveDailyIpRequest(any(), any());
        verify(usageService, never()).reserveDailyCompanionRequest(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void ipDailyLimitBlocksBeforeClassificationAndGeneration() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(usageService.reserveDailyIpRequest(userId, "ip-hash")).thenReturn(AskSanctuaryQuotaDecision.denied(100));

        AskSanctuaryResult result = service.answer(userId, "I feel tired today.", "ip-hash");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.RATE_LIMITED);
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void globalKillSwitchBlocksBeforeAnyModelUse() {
        AskSanctuaryService service = service(false);
        UUID userId = UUID.randomUUID();

        AskSanctuaryResult result = service.answer(userId, "I feel tired today.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.SERVICE_DISABLED);
        verify(usageService, never()).isMisuseLocked(any());
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
    }

    private AskSanctuaryService service() {
        return service(true);
    }

    private AskSanctuaryService service(boolean enabled) {
        return new AskSanctuaryService(
            new AskSanctuaryClassifier(),
            classificationClient,
            modelClient,
            new AskSanctuaryPayloadValidator(),
            repository,
            usageService,
            consentRepository,
            new ObjectMapper(),
            new AskSanctuaryProperties(enabled, new AskSanctuaryProperties.Cache(true, java.time.Duration.ofHours(24)))
        );
    }

    private AskSanctuaryModelOutput modelOutput(String intent) {
        return new AskSanctuaryModelOutput(validModelJson(intent), new AskSanctuaryModelUsage("gpt-test", 100, 120, 220));
    }

    private app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse response(String intent) {
        return new app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse(
            "OK",
            false,
            false,
            null,
            null,
            "Courage for today",
            new app.sanctuary.api.asksanctuary.dto.ScriptureReferenceDto("Isaiah", "41", "10"),
            new app.sanctuary.api.asksanctuary.dto.ScriptureReferenceDto("Matthew", "11", "28"),
            "St. Joseph",
            "Our Father",
            "Bring the day to prayer.",
            "Pray slowly for one minute.",
            intent,
            new app.sanctuary.api.asksanctuary.dto.AskSanctuaryGuardrailDto("NONE", false)
        );
    }

    private String validModelJson(String intent) {
        return """
            {
              "status": "OK",
              "requiresAccount": false,
              "requiresUpgrade": false,
              "message": null,
              "redirectAction": null,
              "theme": "Courage for today",
              "oldTestament": {
                "book": "Isaiah",
                "chapter": "41",
                "verse": "10"
              },
              "newTestament": {
                "book": "Matthew",
                "chapter": "11",
                "verse": "28"
              },
              "saint": "St. Joseph",
              "prayer": "Our Father",
              "reflection": "Bring the day to prayer.",
              "action": "Pray slowly for one minute.",
              "intent": "%s",
              "guardrail": {
                "type": "NONE",
                "triggered": false
              }
            }
            """.formatted(intent);
    }
}
