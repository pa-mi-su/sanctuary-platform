package app.sanctuary.api.content.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.content.dto.IntentionSearchResultDto;
import app.sanctuary.api.content.service.IntentionContentService;

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
}
