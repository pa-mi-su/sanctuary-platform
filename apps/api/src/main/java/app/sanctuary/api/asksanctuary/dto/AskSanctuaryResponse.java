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
        return accountRequired("en");
    }

    public static AskSanctuaryResponse accountRequired(String locale) {
        return new AskSanctuaryResponse(
            "ACCOUNT_REQUIRED",
            true,
            false,
            text(locale, "accountRequired"),
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
        return limitReached(dailyLimit, "en");
    }

    public static AskSanctuaryResponse limitReached(int dailyLimit, String locale) {
        return new AskSanctuaryResponse(
            "LIMIT_REACHED",
            false,
            false,
            text(locale, "limitReached"),
            "TRY_LATER",
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
        return serviceDisabled("en");
    }

    public static AskSanctuaryResponse serviceDisabled(String locale) {
        return new AskSanctuaryResponse(
            "SERVICE_DISABLED",
            false,
            false,
            text(locale, "serviceDisabled"),
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
        return rateLimited("en");
    }

    public static AskSanctuaryResponse rateLimited(String locale) {
        return new AskSanctuaryResponse(
            "RATE_LIMITED",
            false,
            false,
            text(locale, "rateLimited"),
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
        return locked("en");
    }

    public static AskSanctuaryResponse locked(String locale) {
        return new AskSanctuaryResponse(
            "LOCKED",
            false,
            false,
            text(locale, "locked"),
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

    private static String text(String locale, String key) {
        String language = locale == null ? "en" : locale;
        return switch (language) {
            case "es" -> switch (key) {
                case "accountRequired" -> "Compañero Sanctuary está disponible con una cuenta gratuita de Sanctuary para que tu historial de oración y recomendaciones recientes permanezcan privados y consistentes en todos tus dispositivos.";
                case "limitReached" -> "Ya usaste las 3 reflexiones de Compañero Sanctuary de hoy para esta cuenta. Más uso estará disponible pronto; por ahora, vuelve mañana.";
                case "serviceDisabled" -> "Compañero Sanctuary está pausado temporalmente. Inténtalo de nuevo más tarde.";
                case "rateLimited" -> "Compañero Sanctuary está recibiendo demasiadas solicitudes de tu cuenta en este momento. Espera un minuto e inténtalo de nuevo.";
                case "locked" -> "Compañero Sanctuary está pausado para tu cuenta por uso indebido repetido. Vuelve mañana.";
                default -> english(key);
            };
            case "pl" -> switch (key) {
                case "accountRequired" -> "Towarzysz Sanctuary jest dostępny z darmowym kontem Sanctuary, aby Twoja historia modlitwy i ostatnie rekomendacje pozostały prywatne i spójne na wszystkich urządzeniach.";
                case "limitReached" -> "Wykorzystano dzisiejsze 3 refleksje Towarzysza Sanctuary dla tego konta. Większe użycie będzie dostępne wkrótce; na razie wróć jutro.";
                case "serviceDisabled" -> "Towarzysz Sanctuary jest tymczasowo wstrzymany. Spróbuj ponownie później.";
                case "rateLimited" -> "Towarzysz Sanctuary otrzymuje teraz zbyt wiele próśb z Twojego konta. Poczekaj minutę i spróbuj ponownie.";
                case "locked" -> "Towarzysz Sanctuary jest wstrzymany dla Twojego konta z powodu powtarzającego się nadużycia. Wróć jutro.";
                default -> english(key);
            };
            default -> english(key);
        };
    }

    private static String english(String key) {
        return switch (key) {
            case "accountRequired" -> "Sanctuary Companion is available with a free Sanctuary account so your prayer history and recent recommendations can stay private and consistent across devices.";
            case "limitReached" -> "You’ve used today’s 3 Sanctuary Companion reflections for this account. More usage is coming soon; for now, come back tomorrow.";
            case "serviceDisabled" -> "Sanctuary Companion is temporarily paused. Please try again later.";
            case "rateLimited" -> "Sanctuary Companion is receiving too many requests from your account right now. Please wait a minute and try again.";
            case "locked" -> "Sanctuary Companion is paused for your account because of repeated misuse. Please come back tomorrow.";
            default -> "Sanctuary Companion could not complete that request right now.";
        };
    }
}
