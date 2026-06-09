package app.sanctuary.api.content.dto;

import java.util.List;

public record IntentionSearchResultDto(
    List<NovenaSummaryDto> novenas,
    List<SaintSummaryDto> saints
) {
}
