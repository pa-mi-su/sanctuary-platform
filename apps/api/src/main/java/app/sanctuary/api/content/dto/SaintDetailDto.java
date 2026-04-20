package app.sanctuary.api.content.dto;

import java.util.List;

public record SaintDetailDto(
    String id,
    String slug,
    String name,
    int feastMonth,
    int feastDay,
    String feastLabel,
    String summary,
    String biography,
    String imageUrl,
    List<SaintSourceDto> sources
) {
    public SaintDetailDto withSources(List<SaintSourceDto> sources) {
        return new SaintDetailDto(id, slug, name, feastMonth, feastDay, feastLabel, summary, biography, imageUrl, sources);
    }
}
