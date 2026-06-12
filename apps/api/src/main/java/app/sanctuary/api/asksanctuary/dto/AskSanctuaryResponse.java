package app.sanctuary.api.asksanctuary.dto;

public record AskSanctuaryResponse(
    String status,
    boolean requiresAccount,
    boolean requiresUpgrade,
    String message,
    String redirectAction,
    String theme,
    ScriptureReferenceDto oldTestament,
    ScriptureReferenceDto newTestament,
    String saint,
    String prayer,
    String reflection,
    String action,
    String intent,
    AskSanctuaryGuardrailDto guardrail
) {
    public static AskSanctuaryResponse accountRequired() {
        return new AskSanctuaryResponse(
            "ACCOUNT_REQUIRED",
            true,
            false,
            "Ask Sanctuary is available with a free Sanctuary account so your prayer history and recent recommendations can stay private and consistent across devices.",
            "SIGN_IN",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new AskSanctuaryGuardrailDto("ACCOUNT_REQUIRED", true)
        );
    }

    public static AskSanctuaryResponse limitReached(int dailyLimit) {
        return new AskSanctuaryResponse(
            "LIMIT_REACHED",
            false,
            true,
            "You’ve used today’s free Ask Sanctuary messages. Upgrade to continue, or come back tomorrow.",
            "UPGRADE",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new AskSanctuaryGuardrailDto("DAILY_LIMIT", true)
        );
    }

    public static AskSanctuaryResponse serviceDisabled() {
        return new AskSanctuaryResponse(
            "SERVICE_DISABLED",
            false,
            false,
            "Ask Sanctuary is temporarily paused. Please try again later.",
            "TRY_LATER",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new AskSanctuaryGuardrailDto("SERVICE_DISABLED", true)
        );
    }

    public static AskSanctuaryResponse rateLimited() {
        return new AskSanctuaryResponse(
            "RATE_LIMITED",
            false,
            false,
            "Ask Sanctuary is receiving too many requests from your account right now. Please wait a minute and try again.",
            "TRY_LATER",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new AskSanctuaryGuardrailDto("RATE_LIMIT", true)
        );
    }

    public static AskSanctuaryResponse locked() {
        return new AskSanctuaryResponse(
            "LOCKED",
            false,
            false,
            "Ask Sanctuary is paused for your account because of repeated misuse. Please come back tomorrow.",
            "TRY_LATER",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new AskSanctuaryGuardrailDto("MISUSE_LOCK", true)
        );
    }
}
