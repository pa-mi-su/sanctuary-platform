package app.sanctuary.api.auth.dto;

public record AuthSessionResponse(
    String accessToken,
    String idToken,
    String refreshToken,
    String tokenType,
    int expiresIn,
    String email,
    String displayName
) {
}
