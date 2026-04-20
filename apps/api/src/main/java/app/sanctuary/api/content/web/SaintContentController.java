package app.sanctuary.api.content.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.content.dto.SaintDateGroupDto;
import app.sanctuary.api.content.dto.SaintDetailDto;
import app.sanctuary.api.content.dto.SaintSummaryDto;
import app.sanctuary.api.content.service.SaintContentService;

@RestController
@RequestMapping("/content/saints")
public class SaintContentController {

    private final SaintContentService saintContentService;

    public SaintContentController(SaintContentService saintContentService) {
        this.saintContentService = saintContentService;
    }

    @GetMapping("/search")
    public List<SaintSummaryDto> listSaints(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return saintContentService.list(lang, query);
    }

    @GetMapping
    public List<SaintSummaryDto> getSaintsByFeastDay(
        @RequestParam int month,
        @RequestParam int day,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return saintContentService.getByFeastDay(month, day, lang);
    }

    @GetMapping("/range")
    public List<SaintDateGroupDto> getSaintsByDateRange(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate start,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate end,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return saintContentService.getByDateRange(start, end, lang);
    }

    @GetMapping("/{slug}")
    public SaintDetailDto getSaintBySlug(
        @PathVariable String slug,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return saintContentService.getBySlug(slug, lang);
    }
}
