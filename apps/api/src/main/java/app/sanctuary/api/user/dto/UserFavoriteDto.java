package app.sanctuary.api.user.dto;

import java.time.OffsetDateTime;

public record UserFavoriteDto(
    String itemType,
    String itemId,
    OffsetDateTime createdAt
) {
}
