package app.sanctuary.api.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserAccessDto(
    UUID userId,
    String email,
    String displayName,
    boolean admin,
    OffsetDateTime registrationDate,
    OffsetDateTime lastSignInAt
) {
}
