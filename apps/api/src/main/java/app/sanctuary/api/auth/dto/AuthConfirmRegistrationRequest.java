package app.sanctuary.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthConfirmRegistrationRequest(
    @Email @NotBlank String email,
    @NotBlank String code
) {
}
