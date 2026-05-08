package app.sanctuary.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthResetPasswordRequest(
    @Email @NotBlank String email,
    @NotBlank String code,
    @NotBlank
    @Size(min = 10, max = 120)
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{10,120}$",
        message = "Password must be at least 10 characters and include uppercase, lowercase, and number."
    )
    String newPassword
) {
}
