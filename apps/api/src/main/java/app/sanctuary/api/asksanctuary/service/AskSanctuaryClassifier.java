package app.sanctuary.api.asksanctuary.service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;

@Component
public class AskSanctuaryClassifier {

    private static final Pattern SELF_HARM = Pattern.compile("\\b(kill myself|suicide|end my life|hurt myself|harm myself|want to die)\\b");
    private static final Pattern VIOLENCE = Pattern.compile("\\b(murder|kill (him|her|them|my friend|someone)|shoot|stab|beat up|hurt someone|fuck you up)\\b");
    private static final Pattern EMERGENCY = Pattern.compile("\\b(chest pain|can't breathe|cannot breathe|overdose|emergency|call 911|stroke|heart attack)\\b");
    private static final Pattern ABUSE = Pattern.compile("\\b(abuse|abused|hitting me|unsafe at home|domestic violence|being threatened)\\b");
    private static final Pattern IRRELEVANT = Pattern.compile("\\b(shit my pants|shit|poop|pooped|pooping|fart|pee|nigger|sports score|stock price|weather forecast)\\b");

    public Optional<AskSanctuaryIntent> classifyLocally(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase();

        if (SELF_HARM.matcher(normalized).find()) {
            return Optional.of(AskSanctuaryIntent.SELF_HARM_RISK);
        }
        if (VIOLENCE.matcher(normalized).find()) {
            return Optional.of(AskSanctuaryIntent.VIOLENCE_RISK);
        }
        if (EMERGENCY.matcher(normalized).find()) {
            return Optional.of(AskSanctuaryIntent.EMERGENCY_OR_MEDICAL);
        }
        if (ABUSE.matcher(normalized).find()) {
            return Optional.of(AskSanctuaryIntent.ABUSE_OR_DANGER);
        }
        if (IRRELEVANT.matcher(normalized).find()) {
            return Optional.of(AskSanctuaryIntent.NOT_SPIRITUAL_OR_IRRELEVANT);
        }

        return Optional.empty();
    }
}
