package app.sanctuary.api.content.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.content.dto.IntentionSearchResultDto;
import app.sanctuary.api.content.dto.NovenaSummaryDto;
import app.sanctuary.api.content.dto.SearchTermDto;
import app.sanctuary.api.content.service.IntentionContentService;
import java.util.List;

@RestController
@RequestMapping("/content/intentions")
public class IntentionContentController {

    private final IntentionContentService intentionContentService;

    public IntentionContentController(IntentionContentService intentionContentService) {
        this.intentionContentService = intentionContentService;
    }

    @GetMapping("/search")
    public IntentionSearchResultDto searchIntentions(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return intentionContentService.search(lang, query);
    }

    @GetMapping("/terms")
    public List<SearchTermDto> searchIntentionTerms(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return intentionContentService.searchNovenaIntentionTerms(lang, query);
    }

    @GetMapping("/terms/{key}/novenas")
    public List<NovenaSummaryDto> getNovenasByIntention(
        @PathVariable String key,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return intentionContentService.getNovenasByIntention(lang, key);
    }
}
