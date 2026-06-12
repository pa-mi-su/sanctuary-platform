package app.sanctuary.api.asksanctuary.limits;

public record AskSanctuaryMisuseDecision(
    boolean locked,
    int recentMisuseCount
) {
}
