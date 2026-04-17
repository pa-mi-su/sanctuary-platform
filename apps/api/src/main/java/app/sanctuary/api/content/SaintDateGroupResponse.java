package app.sanctuary.api.content;

import java.time.LocalDate;
import java.util.List;

public record SaintDateGroupResponse(
    LocalDate date,
    List<SaintSummaryResponse> saints
) {
}
