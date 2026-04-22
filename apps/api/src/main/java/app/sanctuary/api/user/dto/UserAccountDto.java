package app.sanctuary.api.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserAccountDto(
    UUID id,
    String cognitoSub,
    String email,
    String firstName,
    String lastName,
    String displayName,
    String preferredLanguage,
    String avatarUrl,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
