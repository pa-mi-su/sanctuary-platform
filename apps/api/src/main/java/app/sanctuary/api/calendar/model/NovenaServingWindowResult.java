package app.sanctuary.api.calendar.model;

import java.time.LocalDate;

public record NovenaServingWindowResult(
    String novenaId,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate feastDate
) {
}
