package app.sanctuary.api.asksanctuary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskSanctuaryRequest(
    @NotBlank
    @Size(max = 2000)
    String message
) {
}
