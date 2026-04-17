package app.sanctuary.api.content;

public record NovenaDayDetailResponse(
    int dayNumber,
    String title,
    String scripture,
    String prayer,
    String reflection,
    String body
) {}
