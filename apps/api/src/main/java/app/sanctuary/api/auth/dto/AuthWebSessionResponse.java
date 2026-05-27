package app.sanctuary.api.auth.dto;

public record AuthWebSessionResponse(
    String email,
    String displayName,
    int expiresIn
) {
    public static AuthWebSessionResponse from(AuthSessionResponse session) {
        return new AuthWebSessionResponse(session.email(), session.displayName(), session.expiresIn());
    }
}
