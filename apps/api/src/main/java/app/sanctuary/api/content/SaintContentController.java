package app.sanctuary.api.content;

import java.util.List;

import org.springframework.http.HttpStatus;
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

    @GetMapping
    public List<SaintSummaryResponse> getSaintsByFeastDay(
        @RequestParam int month,
        @RequestParam int day,
        @RequestParam(defaultValue = "en") String lang
    ) {
        validateMonthDay(month, day);
        return saintContentRepository.findByFeastDay(month, day, lang);
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

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static final class NotFoundException extends RuntimeException {
        private NotFoundException(String message) {
            super(message);
        }
    }
}
