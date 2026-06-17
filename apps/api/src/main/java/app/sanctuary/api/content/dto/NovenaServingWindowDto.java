package app.sanctuary.api.content.dto;

import java.time.LocalDate;

public record NovenaServingWindowDto(
    LocalDate startDate,
    LocalDate endDate,
    LocalDate feastDate
) {}
