package app.sanctuary.api.content.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.content.dto.PrayerDetailDto;
import app.sanctuary.api.content.dto.PrayerSummaryDto;
import app.sanctuary.api.content.service.PrayerContentService;

@RestController
@RequestMapping("/content/prayers")
public class PrayerContentController {

    private final PrayerContentService prayerContentService;

    public PrayerContentController(PrayerContentService prayerContentService) {
        this.prayerContentService = prayerContentService;
    }

    @GetMapping
    public List<PrayerSummaryDto> listPrayers(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query,
        @RequestParam(defaultValue = "") String category,
        @RequestParam(defaultValue = "") String excludeCategory
    ) {
        return prayerContentService.list(lang, query, category, excludeCategory);
    }

    @GetMapping("/{slug}")
    public PrayerDetailDto getPrayerBySlug(
        @PathVariable String slug,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return prayerContentService.getBySlug(slug, lang);
    }
}
