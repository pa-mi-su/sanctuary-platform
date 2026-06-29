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
    List<String> patronages,
    List<String> intentions,
    List<SaintSourceDto> sources
) {
    public SaintDetailDto withSources(List<SaintSourceDto> sources) {
        return new SaintDetailDto(id, slug, name, feastMonth, feastDay, feastLabel, summary, biography, imageUrl, patronages, intentions, sources);
    }

    public SaintDetailDto withIntentions(List<String> intentions) {
        return new SaintDetailDto(id, slug, name, feastMonth, feastDay, feastLabel, summary, biography, imageUrl, patronages, intentions, sources);
    }

    public SaintDetailDto withPatronages(List<String> patronages) {
        return new SaintDetailDto(id, slug, name, feastMonth, feastDay, feastLabel, summary, biography, imageUrl, patronages, intentions, sources);
    }
}
