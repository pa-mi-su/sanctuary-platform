package app.sanctuary.api.asksanctuary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskSanctuaryRequest(
    @NotBlank
    @Size(max = 80)
    String message,
    @Size(max = 8)
    String locale
) {
    public AskSanctuaryRequest(String message) {
        this(message, null);
    }
}
