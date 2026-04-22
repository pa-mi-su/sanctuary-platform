package app.sanctuary.api.auth.dto;

public record AuthRegistrationResponse(
    String email,
    String displayName,
    boolean confirmationRequired
) {
}
