package app.sanctuary.api.asksanctuary.repository;

public record AskSanctuaryRecentContent(
    String theme,
    String oldTestamentReference,
    String newTestamentReference,
    String saint,
    String prayer
) {
}
