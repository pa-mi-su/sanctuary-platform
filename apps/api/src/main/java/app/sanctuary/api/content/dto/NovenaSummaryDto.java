package app.sanctuary.api.content.dto;

public record NovenaSummaryDto(
    String id,
    String slug,
    String title,
    String description,
    int durationDays,
    String imageUrl
) {
}
