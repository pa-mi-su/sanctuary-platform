package app.sanctuary.api.content;

public record NovenaSummaryResponse(
    String id,
    String slug,
    String title,
    String description,
    int durationDays,
    String imageUrl
) {
}
