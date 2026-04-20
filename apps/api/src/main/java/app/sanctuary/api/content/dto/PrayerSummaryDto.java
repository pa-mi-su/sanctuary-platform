package app.sanctuary.api.content.dto;

public record PrayerSummaryDto(
    String id,
    String slug,
    String title,
    String bodyPreview,
    String category,
    String imageUrl
) {
}
