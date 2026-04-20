package app.sanctuary.api.content.dto;

import java.util.List;

public record NovenaDetailDto(
    String id,
    String slug,
    String title,
    String description,
    int durationDays,
    String imageUrl,
    List<String> tags,
    List<String> intentions,
    List<NovenaDayDetailDto> days
) {
    public NovenaDetailDto withTags(List<String> tags) {
        return new NovenaDetailDto(id, slug, title, description, durationDays, imageUrl, tags, intentions, days);
    }

    public NovenaDetailDto withIntentions(List<String> intentions) {
        return new NovenaDetailDto(id, slug, title, description, durationDays, imageUrl, tags, intentions, days);
    }

    public NovenaDetailDto withDays(List<NovenaDayDetailDto> days) {
        return new NovenaDetailDto(id, slug, title, description, durationDays, imageUrl, tags, intentions, days);
    }
}
