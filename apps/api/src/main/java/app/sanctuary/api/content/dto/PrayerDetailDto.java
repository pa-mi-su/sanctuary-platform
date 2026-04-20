package app.sanctuary.api.content.dto;

import java.util.List;

public record PrayerDetailDto(
    String id,
    String slug,
    String title,
    String alternateTitle,
    String body,
    String note,
    String category,
    String imageUrl,
    String sourceTitle,
    String sourceType,
    List<String> tags
) {
    public PrayerDetailDto withTags(List<String> tags) {
        return new PrayerDetailDto(
            id,
            slug,
            title,
            alternateTitle,
            body,
            note,
            category,
            imageUrl,
            sourceTitle,
            sourceType,
            tags
        );
    }
}
