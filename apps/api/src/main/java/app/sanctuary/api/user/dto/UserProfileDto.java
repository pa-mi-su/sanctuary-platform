package app.sanctuary.api.user.dto;

public record UserProfileDto(
    String userId,
    String email,
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
    int completedNovenaCount
) {
    public static UserProfileDto from(
        UserAccountDto account,
        UserPreferenceDto preferences,
        UserProfileCountsDto counts
    ) {
        return new UserProfileDto(
            account.id().toString(),
            account.email(),
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
            counts.completedNovenaCount()
        );
    }
}
