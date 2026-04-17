package app.sanctuary.api.content;

public record PrayerSummaryResponse(
    String id,
    String slug,
    String title,
    String bodyPreview,
    String category,
    String imageUrl
) {
}
