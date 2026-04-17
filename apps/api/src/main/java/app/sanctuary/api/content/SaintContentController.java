package app.sanctuary.api.content;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/content/saints")
public class SaintContentController {

    private final SaintContentRepository saintContentRepository;

    public SaintContentController(SaintContentRepository saintContentRepository) {
        this.saintContentRepository = saintContentRepository;
    }

    @GetMapping("/search")
    public List<SaintSummaryResponse> listSaints(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "") String query
    ) {
        return saintContentRepository.list(lang, query);
    }

    @GetMapping
    public List<SaintSummaryResponse> getSaintsByFeastDay(
        @RequestParam int month,
        @RequestParam int day,
        @RequestParam(defaultValue = "en") String lang
    ) {
        validateMonthDay(month, day);
        return saintContentRepository.findByFeastDay(month, day, lang);
    }

    @GetMapping("/range")
    public List<SaintDateGroupResponse> getSaintsByDateRange(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate start,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate end,
        @RequestParam(defaultValue = "en") String lang
    ) {
        validateDateRange(start, end, 62);

        List<SaintDateGroupResponse> response = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            response.add(new SaintDateGroupResponse(
                cursor,
                saintContentRepository.findByFeastDay(cursor.getMonthValue(), cursor.getDayOfMonth(), lang)
            ));
            cursor = cursor.plusDays(1);
        }
        return response;
    }

    @GetMapping("/{slug}")
    public SaintDetailResponse getSaintBySlug(
        @PathVariable String slug,
        @RequestParam(defaultValue = "en") String lang
    ) {
        return saintContentRepository.findBySlug(slug, lang)
            .orElseThrow(() -> new NotFoundException("No saint found for slug: " + slug));
    }

    private void validateMonthDay(int month, int day) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must be between 1 and 12");
        }
        if (day < 1 || day > 31) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "day must be between 1 and 31");
        }
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
