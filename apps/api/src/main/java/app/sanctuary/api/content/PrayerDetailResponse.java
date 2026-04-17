package app.sanctuary.api.content;

import java.util.List;

public record PrayerDetailResponse(
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
    public PrayerDetailResponse withTags(List<String> tags) {
        return new PrayerDetailResponse(
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
