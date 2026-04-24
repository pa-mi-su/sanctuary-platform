package app.sanctuary.api.content.dto;

import java.util.List;

public record NovenaSummaryDto(
    String id,
    String slug,
    String title,
    String description,
    int durationDays,
    String imageUrl,
    List<String> intentions
) {
}
