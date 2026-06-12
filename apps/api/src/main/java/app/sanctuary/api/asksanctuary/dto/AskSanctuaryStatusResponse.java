package app.sanctuary.api.asksanctuary.dto;

public record AskSanctuaryStatusResponse(
    String disclaimerVersion,
    boolean disclaimerAccepted,
    boolean available,
    String unavailableMessage
) {
    public AskSanctuaryStatusResponse(String disclaimerVersion, boolean disclaimerAccepted) {
        this(disclaimerVersion, disclaimerAccepted, true, null);
    }
}
