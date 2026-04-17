package app.sanctuary.api.content;

public record SaintSummaryResponse(
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
