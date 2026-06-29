package app.sanctuary.api.content.dto;

import java.util.List;

public record SearchTermDto(
    String key,
    String label,
    int resultCount,
    List<String> imageUrls
) {
}
