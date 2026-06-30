package app.sanctuary.api.content.dto;

import java.util.List;

public record SaintSummaryDto(
    String id,
    String slug,
    String name,
    int feastMonth,
    int feastDay,
    String feastLabel,
    String summary,
    String imageUrl,
    List<String> patronages,
    List<String> intentions
) {
}
