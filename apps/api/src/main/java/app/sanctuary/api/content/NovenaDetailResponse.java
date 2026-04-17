package app.sanctuary.api.content;

import java.util.List;

public record NovenaDetailResponse(
    String id,
    String slug,
    String title,
    String description,
    int durationDays,
    String imageUrl,
    List<String> tags,
    List<String> intentions,
    List<NovenaDayDetailResponse> days
) {
    public NovenaDetailResponse withTags(List<String> tags) {
        return new NovenaDetailResponse(id, slug, title, description, durationDays, imageUrl, tags, intentions, days);
    }

    public NovenaDetailResponse withIntentions(List<String> intentions) {
        return new NovenaDetailResponse(id, slug, title, description, durationDays, imageUrl, tags, intentions, days);
    }

    public NovenaDetailResponse withDays(List<NovenaDayDetailResponse> days) {
        return new NovenaDetailResponse(id, slug, title, description, durationDays, imageUrl, tags, intentions, days);
    }
}
