package app.sanctuary.api.asksanctuary.model;

public enum AskSanctuaryGuardrailType {
    NONE,
    ACCOUNT_REQUIRED,
    SERVICE_DISABLED,
    DAILY_LIMIT,
    RATE_LIMIT,
    MISUSE_LOCK,
    IRRELEVANT,
    VIOLENCE_RISK,
    SELF_HARM_RISK,
    EMERGENCY_OR_MEDICAL,
    ABUSE_OR_DANGER,
    MODEL_FALLBACK
}
