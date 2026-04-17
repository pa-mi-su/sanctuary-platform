package app.sanctuary.api.calendar.model;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

public record LiturgicalDayResult(
    LocalDate date,
    LiturgicalSeason season,
    String primaryRank,
    List<String> observances,
    URI readingsUrl,
    RankType rankType
) {
}
