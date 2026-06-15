package app.sanctuary.api.asksanctuary.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;

class AskSanctuaryClassifierTest {

    private final AskSanctuaryClassifier classifier = new AskSanctuaryClassifier();

    @Test
    void leavesNormalCompanionInputsForModelClassification() {
        assertThat(classifier.classifyLocally("worried, hopeful, focused")).isEmpty();
        assertThat(classifier.classifyLocally("My dad had an amputation and worried")).isEmpty();
        assertThat(classifier.classifyLocally("My dog just died.")).isEmpty();
        assertThat(classifier.classifyLocally("My wife cheated on me.")).isEmpty();
        assertThat(classifier.classifyLocally("tired")).isEmpty();
        assertThat(classifier.classifyLocally("I am overwhelmed and burned out.")).isEmpty();
    }

    @Test
    void classifiesGuardrailInputsBeforeModelUse() {
        assertThat(classifier.classifyLocally("I want to murder my friend."))
            .contains(AskSanctuaryIntent.VIOLENCE_RISK);
        assertThat(classifier.classifyLocally("I will fuck you up."))
            .contains(AskSanctuaryIntent.VIOLENCE_RISK);
        assertThat(classifier.classifyLocally("I want to kill myself."))
            .contains(AskSanctuaryIntent.SELF_HARM_RISK);
        assertThat(classifier.classifyLocally("I have chest pain and cannot breathe."))
            .contains(AskSanctuaryIntent.EMERGENCY_OR_MEDICAL);
        assertThat(classifier.classifyLocally("I am unsafe at home."))
            .contains(AskSanctuaryIntent.ABUSE_OR_DANGER);
    }

    @Test
    void classifiesObviousIrrelevantInputsBeforeModelUse() {
        assertThat(classifier.classifyLocally("I just shit my pants."))
            .contains(AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT);
        assertThat(classifier.classifyLocally("poop"))
            .contains(AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT);
        assertThat(classifier.classifyLocally("What is the sports score?"))
            .contains(AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT);
    }
}
