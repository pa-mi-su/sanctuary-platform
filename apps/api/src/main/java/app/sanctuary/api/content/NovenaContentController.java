package app.sanctuary.api.content;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/content/novenas")
public class NovenaContentController {

    private final NovenaCalendarContentRepository novenaContentRepository;

    public NovenaContentController(NovenaCalendarContentRepository novenaContentRepository) {
        this.novenaContentRepository = novenaContentRepository;
    }

    @GetMapping
    public List<NovenaSummaryResponse> listNovenas(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return novenaContentRepository.list(lang, query);
    }

    @GetMapping("/intentions")
    public List<NovenaSummaryResponse> searchNovenasByIntentions(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return novenaContentRepository.listByIntentions(lang, query);
    }

    @GetMapping("/calendar")
    public List<NovenaCalendarDateResponse> getNovenasByDateRange(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate start,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate end,
        @RequestParam(defaultValue = "en") String lang
    ) {
        validateDateRange(start, end, 62);
        return novenaContentRepository.calendarRange(start, end, lang);
    }

    @GetMapping("/{slug}")
    public NovenaDetailResponse getNovenaBySlug(
        @PathVariable String slug,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return novenaContentRepository.findBySlug(slug, lang)
            .orElseThrow(() -> new NotFoundException("No novena found for slug: " + slug));
    }

    private void validateDateRange(LocalDate start, LocalDate end, int maxDaysInclusive) {
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "end must be on or after start");
        }

        if (start.plusDays(maxDaysInclusive - 1L).isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date range exceeds maximum supported span");
        }
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static final class NotFoundException extends RuntimeException {
        private NotFoundException(String message) {
            super(message);
        }
    }
}
