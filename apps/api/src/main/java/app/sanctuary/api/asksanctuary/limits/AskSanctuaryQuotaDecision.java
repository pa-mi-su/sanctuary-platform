package app.sanctuary.api.asksanctuary.limits;

public record AskSanctuaryQuotaDecision(
    boolean allowed,
    int dailyLimit,
    int usedToday,
    boolean unlimited
) {
    public static AskSanctuaryQuotaDecision allowed(int dailyLimit, int usedToday) {
        return new AskSanctuaryQuotaDecision(true, dailyLimit, usedToday, false);
    }

    public static AskSanctuaryQuotaDecision unlimitedAllowed() {
        return new AskSanctuaryQuotaDecision(true, Integer.MAX_VALUE, 0, true);
    }

    public static AskSanctuaryQuotaDecision denied(int dailyLimit) {
        return new AskSanctuaryQuotaDecision(false, dailyLimit, dailyLimit, false);
    }
}
