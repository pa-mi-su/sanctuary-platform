package app.sanctuary.api.content;

import java.time.LocalDate;
import java.util.List;

public record NovenaCalendarDateResponse(
    LocalDate date,
    List<NovenaSummaryResponse> novenas
) {
}
