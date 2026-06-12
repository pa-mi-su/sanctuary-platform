package app.sanctuary.api.asksanctuary.model;

public enum AskSanctuaryStatus {
    OK,
    ACCOUNT_REQUIRED,
    SERVICE_DISABLED,
    LIMIT_REACHED,
    RATE_LIMITED,
    LOCKED,
    REDIRECT,
    GUARDED,
    FALLBACK
}
