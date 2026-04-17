package app.sanctuary.api.content;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/content/prayers")
public class PrayerContentController {

    private final PrayerContentRepository prayerContentRepository;

    public PrayerContentController(PrayerContentRepository prayerContentRepository) {
        this.prayerContentRepository = prayerContentRepository;
    }

    @GetMapping
    public List<PrayerSummaryResponse> listPrayers(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return prayerContentRepository.list(lang, query);
    }

    @GetMapping("/{slug}")
    public PrayerDetailResponse getPrayerBySlug(
        @PathVariable String slug,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return prayerContentRepository.findBySlug(slug, lang)
            .orElseThrow(() -> new NotFoundException("No prayer found for slug: " + slug));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static final class NotFoundException extends RuntimeException {
        private NotFoundException(String message) {
            super(message);
        }
    }
}
