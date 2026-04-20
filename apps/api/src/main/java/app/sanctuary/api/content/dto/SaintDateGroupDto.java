package app.sanctuary.api.content.dto;

import java.time.LocalDate;
import java.util.List;

public record SaintDateGroupDto(
    LocalDate date,
    List<SaintSummaryDto> saints
) {
}
