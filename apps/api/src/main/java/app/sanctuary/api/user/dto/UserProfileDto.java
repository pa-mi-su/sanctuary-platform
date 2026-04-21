package app.sanctuary.api.user.dto;

public record UserProfileDto(
    String userId,
    String email,
    String displayName
) {
}
