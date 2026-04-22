package app.sanctuary.api.user.dto;

public record UserProfileDto(
    String userId,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String preferredLanguage,
    String avatarUrl,
    String timeZoneId,
    boolean novenaRemindersEnabled,
    boolean feastRemindersEnabled,
    boolean emailUpdatesEnabled,
    boolean onboardingCompleted,
    int favoriteSaintCount,
    int favoriteNovenaCount,
    int favoritePrayerCount,
    int activeNovenaCount,
    int completedNovenaCount,
    int currentStreakDays,
    int longestStreakDays,
    java.time.LocalDate lastActiveDate
) {
    public static UserProfileDto from(
        UserAccountDto account,
        UserPreferenceDto preferences,
        UserProfileCountsDto counts,
        UserStreakSummaryDto streakSummary
    ) {
        return new UserProfileDto(
            account.id().toString(),
            account.email(),
            account.firstName(),
            account.lastName(),
            account.displayName(),
            account.preferredLanguage(),
            account.avatarUrl(),
            preferences.timeZoneId(),
            preferences.novenaRemindersEnabled(),
            preferences.feastRemindersEnabled(),
            preferences.emailUpdatesEnabled(),
            preferences.onboardingCompleted(),
            counts.favoriteSaintCount(),
            counts.favoriteNovenaCount(),
            counts.favoritePrayerCount(),
            counts.activeNovenaCount(),
            counts.completedNovenaCount(),
            streakSummary.currentStreakDays(),
            streakSummary.longestStreakDays(),
            streakSummary.lastActiveDate()
        );
    }
}
