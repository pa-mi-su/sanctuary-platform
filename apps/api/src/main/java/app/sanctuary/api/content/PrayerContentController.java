package app.sanctuary.api.content;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.content.service.PrayerContentService;

@RestController
@RequestMapping("/content/prayers")
public class PrayerContentController {

    private final PrayerContentService prayerContentService;

    public PrayerContentController(PrayerContentService prayerContentService) {
        this.prayerContentService = prayerContentService;
    }

    @GetMapping
    public List<PrayerSummaryResponse> listPrayers(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return prayerContentService.list(lang, query);
    }

    @GetMapping("/{slug}")
    public PrayerDetailResponse getPrayerBySlug(
        @PathVariable String slug,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return prayerContentService.getBySlug(slug, lang);
    }
}
