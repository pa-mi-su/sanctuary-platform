package app.sanctuary.api.asksanctuary.limits;

public record AskSanctuaryEntitlement(
    String tier,
    Integer dailyLimitOverride,
    boolean unlimited
) {
    public static AskSanctuaryEntitlement free() {
        return new AskSanctuaryEntitlement("FREE", null, false);
    }
}
