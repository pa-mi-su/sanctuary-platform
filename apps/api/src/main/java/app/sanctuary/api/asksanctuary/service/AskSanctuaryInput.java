package app.sanctuary.api.asksanctuary.service;

import java.util.List;

public record AskSanctuaryInput(
    List<String> words,
    String normalizedMessage
) {
}
