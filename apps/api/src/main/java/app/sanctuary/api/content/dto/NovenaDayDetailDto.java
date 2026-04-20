package app.sanctuary.api.content.dto;

public record NovenaDayDetailDto(
    int dayNumber,
    String title,
    String scripture,
    String prayer,
    String reflection,
    String body
) {}
