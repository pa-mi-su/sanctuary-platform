package app.sanctuary.api.user.dto;

import jakarta.validation.constraints.*;

public record ContentReminder(
    @NotBlank @Pattern(regexp = "prayers|novenas") String kind,
    @NotBlank @Size(max = 200) @Pattern(regexp = "[A-Za-z0-9_-]+") String slug,
    @NotBlank @Size(max = 300) String title,
    @NotNull @Min(0) @Max(23) Integer hour,
    @NotNull @Min(0) @Max(59) Integer minute
) {}
