package app.sanctuary.api.user.dto;

public record UserProfileCountsDto(
    int favoriteSaintCount,
    int favoriteNovenaCount,
    int favoritePrayerCount,
    int activeNovenaCount,
    int completedNovenaCount
) {
}
