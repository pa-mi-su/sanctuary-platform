package app.sanctuary.api.asksanctuary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.List;

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
import app.sanctuary.api.asksanctuary.repository.AskSanctuaryRecentContent;
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
        assertThat(status.unavailableMessage()).isEqualTo("Sanctuary Companion is temporarily unavailable. Please try again later.");
    }

    @Test
    void statusReturnsUnavailableWhenModelClientsAreNotConfigured() {
        AskSanctuaryService service = service(true, false, true);
        UUID userId = UUID.randomUUID();
        when(consentRepository.hasAccepted(userId, "ASK_SANCTUARY", "v1")).thenReturn(true);

        var status = service.status(userId);

        assertThat(status.disclaimerVersion()).isEqualTo("v1");
        assertThat(status.disclaimerAccepted()).isTrue();
        assertThat(status.available()).isFalse();
        assertThat(status.unavailableMessage()).isEqualTo("Sanctuary Companion is temporarily unavailable. Please try again later.");
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
        when(usageService.recordMisuse(userId, AskSanctuaryGuardrailType.VIOLENCE_RISK))
            .thenReturn(new AskSanctuaryMisuseDecision(false, 1));

        AskSanctuaryResult result = service.answer(userId, "I want to murder my friend.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.GUARDED);
        assertThat(result.intent()).isEqualTo(AskSanctuaryIntent.VIOLENCE_RISK);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.VIOLENCE_RISK);
        assertThat(result.response().guardrail().triggered()).isTrue();
        verify(classificationClient, never()).classify(any());
        verify(usageService, never()).isMisuseLocked(any());
        verify(usageService, never()).isBurstLimited(any());
        verify(modelClient, never()).generate(any());
        verify(repository).save(any(AskSanctuarySessionLog.class));
    }

    @Test
    void violenceSafetyResponseBypassesExistingMisuseLockButStillRecordsMisuse() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.recordMisuse(userId, AskSanctuaryGuardrailType.VIOLENCE_RISK))
            .thenReturn(new AskSanctuaryMisuseDecision(true, 3));

        AskSanctuaryResult result = service.answer(userId, "I want to kill someone.");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.GUARDED);
        assertThat(result.intent()).isEqualTo(AskSanctuaryIntent.VIOLENCE_RISK);
        assertThat(result.response().message()).contains("Do not harm anyone");
        verify(usageService, never()).isMisuseLocked(any());
        verify(usageService).recordMisuse(userId, AskSanctuaryGuardrailType.VIOLENCE_RISK);
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
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
        assertThat(result.response().message()).contains("one simple feeling word");
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void normalMessageClassifiesThenCallsModelAndPersistsValidatedPayload() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("focused"))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.WORK_OR_DISCERNMENT));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.allowed(3, 1));
        when(repository.findRecentContent(eq(userId), any(OffsetDateTime.class), eq(12)))
            .thenReturn(List.of(new AskSanctuaryRecentContent(
                "Rest in Christ",
                "Psalms 23:1",
                "Luke 10:42",
                "St. Teresa of Avila",
                "Prayer for Peace"
            )));
        when(modelClient.generate(any())).thenReturn(modelOutput("WORK_OR_DISCERNMENT"));

        AskSanctuaryResult result = service.answer(userId, "focused", null, "pl");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.OK);
        assertThat(result.intent()).isEqualTo(AskSanctuaryIntent.WORK_OR_DISCERNMENT);
        assertThat(result.response().theme()).isEqualTo("Courage for today");
        verify(classificationClient).classify("focused");
        ArgumentCaptor<AskSanctuaryModelRequest> modelRequestCaptor = ArgumentCaptor.forClass(AskSanctuaryModelRequest.class);
        verify(modelClient).generate(modelRequestCaptor.capture());
        assertThat(modelRequestCaptor.getValue().locale()).isEqualTo("pl");
        assertThat(modelRequestCaptor.getValue().variationGuidance())
            .contains("last 30 days")
            .contains("Psalms 23:1")
            .contains("St. Teresa of Avila");

        ArgumentCaptor<AskSanctuarySessionLog> logCaptor = ArgumentCaptor.forClass(AskSanctuarySessionLog.class);
        verify(repository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().responsePayload()).contains("\"theme\":\"Courage for today\"");
        assertThat(logCaptor.getValue().generationUsage().model()).isEqualTo("gpt-test");
    }

    @Test
    void sentenceStyleInputRedirectsWithoutCallingModelOrQuota() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();

        AskSanctuaryResult result = service.answer(userId, "I feel anxious about today");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.REDIRECT);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.IRRELEVANT);
        assertThat(result.response().message()).contains("one simple feeling word");
        verify(classificationClient, never()).classify(any());
        verify(usageService, never()).reserveDailyCompanionRequest(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void invalidModelPayloadRetriesOnceThenFallsBack() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("worried"))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.LIFE_CONCERN));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.allowed(3, 1));
        when(modelClient.generate(any()))
            .thenReturn(new AskSanctuaryModelOutput("{\"status\":\"OK\"}", AskSanctuaryModelUsage.none()))
            .thenReturn(new AskSanctuaryModelOutput("{\"status\":\"OK\"}", AskSanctuaryModelUsage.none()));

        AskSanctuaryResult result = service.answer(userId, "worried");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.FALLBACK);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.MODEL_FALLBACK);
        assertThat(result.response().oldTestament().book()).isEqualTo("Psalms");
        verify(modelClient, times(2)).generate(any());
    }

    @Test
    void repeatedRecentContentRetriesWithVariationGuidance() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("tired"))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.LIFE_CONCERN));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.allowed(3, 1));
        when(repository.findRecentContent(eq(userId), any(OffsetDateTime.class), eq(12)))
            .thenReturn(List.of(new AskSanctuaryRecentContent(
                "Courage for today",
                "Isaiah 41:10",
                "Matthew 11:28",
                "St. Joseph",
                "Our Father"
            )));
        when(modelClient.generate(any()))
            .thenReturn(modelOutput("LIFE_CONCERN"))
            .thenReturn(new AskSanctuaryModelOutput("""
                {
                  "status": "OK",
                  "requiresAccount": false,
                  "requiresUpgrade": false,
                  "message": null,
                  "redirectAction": null,
                  "theme": "Rest for the weary",
                  "oldTestament": {
                    "book": "Psalms",
                    "chapter": "62",
                    "verse": "1"
                  },
                  "newTestament": {
                    "book": "Mark",
                    "chapter": "6",
                    "verse": "31"
                  },
                  "saint": "St. Teresa of Avila",
                  "prayer": "Prayer for Peace",
                  "reflection": "Bring your weariness to God without pretending.",
                  "action": "Pause for one quiet breath and pray for strength.",
                  "intent": "LIFE_CONCERN",
                  "guardrail": {
                    "type": "NONE",
                    "triggered": false
                  }
                }
                """, AskSanctuaryModelUsage.none()));

        AskSanctuaryResult result = service.answer(userId, "tired");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.OK);
        assertThat(result.response().theme()).isEqualTo("Rest for the weary");
        verify(modelClient, times(2)).generate(any());
    }

    @Test
    void dailyLimitReachedDoesNotCallModelAndPersistsLimitResponse() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("worried"))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.LIFE_CONCERN));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.denied(3));

        AskSanctuaryResult result = service.answer(userId, "worried");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.LIMIT_REACHED);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.DAILY_LIMIT);
        assertThat(result.response().requiresUpgrade()).isFalse();
        assertThat(result.response().redirectAction()).isEqualTo("TRY_LATER");
        verify(classificationClient).classify("worried");
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

        AskSanctuaryResult result = service.answer(userId, "worried");

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

        AskSanctuaryResult result = service.answer(userId, "worried");

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
        when(classificationClient.classify("tired"))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.LIFE_CONCERN));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.allowed(3, 1));
        when(modelClient.generate(any())).thenReturn(modelOutput("LIFE_CONCERN"));

        AskSanctuaryResult result = service.answer(userId, "tired");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.OK);
        assertThat(result.intent()).isEqualTo(AskSanctuaryIntent.LIFE_CONCERN);
        verify(classificationClient).classify("tired");
        verify(modelClient).generate(any(AskSanctuaryModelRequest.class));
    }

    @Test
    void modelClassifiedIrrelevantMessageRedirectsWithoutGeneratingAnswer() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("football"))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.recordMisuse(userId, AskSanctuaryGuardrailType.IRRELEVANT))
            .thenReturn(new AskSanctuaryMisuseDecision(false, 1));

        AskSanctuaryResult result = service.answer(userId, "football");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.REDIRECT);
        assertThat(result.guardrailType()).isEqualTo(AskSanctuaryGuardrailType.IRRELEVANT);
        verify(classificationClient).classify("football");
        verify(modelClient, never()).generate(any());
    }

    @Test
    void cachedResponseCountsAgainstQuotaButAvoidsGeneration() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(classificationClient.classify("tired"))
            .thenReturn(new AskSanctuaryClassification(AskSanctuaryIntent.LIFE_CONCERN));
        when(usageService.reserveDailyIpRequest(userId, null)).thenReturn(AskSanctuaryQuotaDecision.allowed(100, 1));
        when(usageService.reserveDailyCompanionRequest(userId)).thenReturn(AskSanctuaryQuotaDecision.allowed(3, 1));
        when(repository.findRecentReusableResponse(eq(userId), any(), any()))
            .thenReturn(java.util.Optional.of(new AskSanctuaryCachedResponse(
                AskSanctuaryIntent.LIFE_CONCERN,
                AskSanctuaryGuardrailType.NONE,
                false,
                AskSanctuaryStatus.OK,
                response("LIFE_CONCERN")
            )));

        AskSanctuaryResult result = service.answer(userId, "tired");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.OK);
        assertThat(result.reusedResponse()).isTrue();
        verify(classificationClient).classify("tired");
        verify(usageService).reserveDailyIpRequest(userId, null);
        verify(usageService).reserveDailyCompanionRequest(userId);
        verify(modelClient, never()).generate(any());
    }

    @Test
    void ipDailyLimitBlocksBeforeClassificationAndGeneration() {
        AskSanctuaryService service = service();
        UUID userId = UUID.randomUUID();
        when(usageService.isMisuseLocked(userId)).thenReturn(false);
        when(usageService.isBurstLimited(userId)).thenReturn(false);
        when(usageService.reserveDailyIpRequest(userId, "ip-hash")).thenReturn(AskSanctuaryQuotaDecision.denied(100));

        AskSanctuaryResult result = service.answer(userId, "tired", "ip-hash");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.RATE_LIMITED);
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
    }

    @Test
    void globalKillSwitchBlocksBeforeAnyModelUse() {
        AskSanctuaryService service = service(false);
        UUID userId = UUID.randomUUID();

        AskSanctuaryResult result = service.answer(userId, "tired");

        assertThat(result.status()).isEqualTo(AskSanctuaryStatus.SERVICE_DISABLED);
        verify(usageService, never()).isMisuseLocked(any());
        verify(classificationClient, never()).classify(any());
        verify(modelClient, never()).generate(any());
    }

    private AskSanctuaryService service() {
        return service(true);
    }

    private AskSanctuaryService service(boolean enabled) {
        return service(enabled, true);
    }

    private AskSanctuaryService service(boolean enabled, boolean configured) {
        return service(enabled, configured, configured);
    }

    private AskSanctuaryService service(boolean enabled, boolean modelConfigured, boolean classificationConfigured) {
        if (enabled) {
            when(modelClient.isConfigured()).thenReturn(modelConfigured);
            if (modelConfigured) {
                when(classificationClient.isConfigured()).thenReturn(classificationConfigured);
            }
        }
        return new AskSanctuaryService(
            new AskSanctuaryClassifier(),
            classificationClient,
            modelClient,
            new AskSanctuaryInputValidator(),
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
