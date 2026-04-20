package app.sanctuary.api.content.dto;

public record SaintSummaryDto(
    String id,
    String slug,
    String name,
    int feastMonth,
    int feastDay,
    String feastLabel,
    String summary,
    String imageUrl
) {
}
