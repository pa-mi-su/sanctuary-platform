package app.sanctuary.api.asksanctuary.service;

import org.springframework.stereotype.Component;

import app.sanctuary.api.asksanctuary.dto.AskSanctuaryResponse;
import app.sanctuary.api.asksanctuary.dto.ScriptureReferenceDto;
import app.sanctuary.api.asksanctuary.model.AskSanctuaryIntent;

@Component
public class AskSanctuaryPayloadValidator {

    public void validate(AskSanctuaryResponse response) {
        require(response, "response");
        require(response.status(), "status");
        if (!"OK".equals(response.status())) {
            throw new IllegalArgumentException("Ask Sanctuary model response status must be OK.");
        }
        if (response.requiresAccount()) {
            throw new IllegalArgumentException("Ask Sanctuary model response cannot require account.");
        }
        if (response.requiresUpgrade()) {
            throw new IllegalArgumentException("Ask Sanctuary model response cannot require upgrade.");
        }
        require(response.theme(), "theme");
        validateReference(response.oldTestament(), "oldTestament");
        validateReference(response.newTestament(), "newTestament");
        require(response.saint(), "saint");
        require(response.prayer(), "prayer");
        require(response.reflection(), "reflection");
        require(response.action(), "action");
        require(response.intent(), "intent");
        validateIntent(response.intent());
        require(response.guardrail(), "guardrail");
        require(response.guardrail().type(), "guardrail.type");
        if (!"NONE".equals(response.guardrail().type()) || response.guardrail().triggered()) {
            throw new IllegalArgumentException("Ask Sanctuary model response guardrail must be NONE.");
        }
    }

    private void validateIntent(String intent) {
        try {
            AskSanctuaryIntent.valueOf(intent);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Ask Sanctuary model response has unsupported intent.", exception);
        }
    }

    private void validateReference(ScriptureReferenceDto reference, String fieldName) {
        require(reference, fieldName);
        require(reference.book(), fieldName + ".book");
        require(reference.chapter(), fieldName + ".chapter");
        require(reference.verse(), fieldName + ".verse");
    }

    private void require(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Ask Sanctuary model response missing " + fieldName + ".");
        }
        if (value instanceof String string && string.isBlank()) {
            throw new IllegalArgumentException("Ask Sanctuary model response has blank " + fieldName + ".");
        }
    }
}
