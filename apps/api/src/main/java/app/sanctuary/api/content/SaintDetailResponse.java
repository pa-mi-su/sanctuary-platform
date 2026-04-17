package app.sanctuary.api.content;

import java.util.List;

public record SaintDetailResponse(
    String id,
    String slug,
    String name,
    int feastMonth,
    int feastDay,
    String feastLabel,
    String summary,
    String biography,
    String imageUrl,
    List<SaintSourceResponse> sources
) {
    public SaintDetailResponse withSources(List<SaintSourceResponse> sources) {
        return new SaintDetailResponse(id, slug, name, feastMonth, feastDay, feastLabel, summary, biography, imageUrl, sources);
    }
}
