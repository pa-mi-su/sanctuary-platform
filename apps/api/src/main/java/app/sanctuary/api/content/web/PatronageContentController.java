package app.sanctuary.api.content.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.content.dto.SaintSummaryDto;
import app.sanctuary.api.content.dto.SearchTermDto;
import app.sanctuary.api.content.service.PatronageContentService;

@RestController
@RequestMapping("/content/patronages")
public class PatronageContentController {

    private final PatronageContentService patronageContentService;

    public PatronageContentController(PatronageContentService patronageContentService) {
        this.patronageContentService = patronageContentService;
    }

    @GetMapping("/terms")
    public List<SearchTermDto> searchPatronageTerms(
        @RequestParam(defaultValue = "") String query
    ) {
        return patronageContentService.searchTerms(query);
    }

    @GetMapping("/terms/{key}/saints")
    public List<SaintSummaryDto> getSaintsByPatronage(
        @PathVariable String key,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return patronageContentService.getSaintsByPatronage(lang, key);
    }
}
