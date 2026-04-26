package app.sanctuary.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRefreshRequest(
    @NotBlank String refreshToken
) {
}
