package app.sanctuary.api.news.dto;

import java.time.OffsetDateTime;

public record ChurchNewsArticleDto(
    String id,
    String title,
    String summary,
    String sourceName,
    String canonicalUrl,
    String imageUrl,
    String imageAlt,
    String imageCredit,
    String licenseName,
    String licenseUrl,
    OffsetDateTime publishedAt,
    String language
) {}
