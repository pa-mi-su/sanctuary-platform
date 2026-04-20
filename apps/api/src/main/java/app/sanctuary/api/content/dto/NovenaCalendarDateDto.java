package app.sanctuary.api.content.dto;

import java.time.LocalDate;
import java.util.List;

public record NovenaCalendarDateDto(
    LocalDate date,
    List<NovenaSummaryDto> novenas,
    NovenaSummaryDto startingNovena
) {
}
