package app.sanctuary.api.calendar.web;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.calendar.model.LiturgicalAnchorKey;
import app.sanctuary.api.calendar.model.LiturgicalDayResult;
import app.sanctuary.api.calendar.model.NovenaServingWindowResult;
import app.sanctuary.api.calendar.service.LiturgicalAnchorService;
import app.sanctuary.api.calendar.service.LiturgicalCalendarService;
import app.sanctuary.api.calendar.repository.NovenaServingRuleRepository;
import app.sanctuary.api.calendar.service.NovenaServingWindowResolver;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private final LiturgicalCalendarService liturgicalCalendarService;
    private final LiturgicalAnchorService liturgicalAnchorService;
    private final NovenaServingRuleRepository novenaServingRuleRepository;
    private final NovenaServingWindowResolver novenaServingWindowResolver;

    public CalendarController(
        LiturgicalCalendarService liturgicalCalendarService,
        LiturgicalAnchorService liturgicalAnchorService,
        NovenaServingRuleRepository novenaServingRuleRepository,
        NovenaServingWindowResolver novenaServingWindowResolver
    ) {
        this.liturgicalCalendarService = liturgicalCalendarService;
        this.liturgicalAnchorService = liturgicalAnchorService;
        this.novenaServingRuleRepository = novenaServingRuleRepository;
        this.novenaServingWindowResolver = novenaServingWindowResolver;
    }

    @GetMapping("/day/{date}")
    public LiturgicalDayResult getLiturgicalDay(
        @PathVariable
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date
    ) {
        return liturgicalCalendarService.getLiturgicalDay(date);
    }

    @GetMapping("/range")
    public List<LiturgicalDayResult> getLiturgicalRange(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate start,
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate end
    ) {
        validateDateRange(start, end, 62);

        List<LiturgicalDayResult> days = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            days.add(liturgicalCalendarService.getLiturgicalDay(cursor));
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    @GetMapping("/anchors/{year}")
    public Map<String, LocalDate> getAnchors(@PathVariable int year) {
        if (year < 1900 || year > 4099) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year must be between 1900 and 4099");
        }

        TreeMap<String, LocalDate> response = new TreeMap<>();
        for (Map.Entry<LiturgicalAnchorKey, LocalDate> entry : liturgicalAnchorService.getAnchors(year).entrySet()) {
            response.put(entry.getKey().name().toLowerCase(), entry.getValue());
        }
        return response;
    }

    @GetMapping("/novenas/{novenaId}/window/{year}")
    public NovenaServingWindowResult getNovenaServingWindow(@PathVariable String novenaId, @PathVariable int year) {
        if (year < 1900 || year > 4099) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "year must be between 1900 and 4099");
        }

        return novenaServingRuleRepository.findByNovenaId(novenaId)
            .map(rule -> novenaServingWindowResolver.resolve(rule, year))
            .orElseThrow(() -> new NotFoundException("No novena serving rule found for novena id: " + novenaId));
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
