package app.sanctuary.api.content.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.content.dto.NovenaCalendarDateDto;
import app.sanctuary.api.content.dto.NovenaDetailDto;
import app.sanctuary.api.content.dto.NovenaSummaryDto;
import app.sanctuary.api.content.service.NovenaContentService;

@RestController
@RequestMapping("/content/novenas")
public class NovenaContentController {

    private final NovenaContentService novenaContentService;

    public NovenaContentController(NovenaContentService novenaContentService) {
        this.novenaContentService = novenaContentService;
    }

    @GetMapping
    public List<NovenaSummaryDto> listNovenas(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return novenaContentService.list(lang, query);
    }

    @GetMapping("/intentions")
    public List<NovenaSummaryDto> searchNovenasByIntentions(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return novenaContentService.listByIntentions(lang, query);
    }

    @GetMapping("/calendar")
    public List<NovenaCalendarDateDto> getNovenasByDateRange(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate start,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate end,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return novenaContentService.getCalendarRange(start, end, lang);
    }

    @GetMapping("/{slug}")
    public NovenaDetailDto getNovenaBySlug(
        @PathVariable String slug,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return novenaContentService.getBySlug(slug, lang);
    }
}
