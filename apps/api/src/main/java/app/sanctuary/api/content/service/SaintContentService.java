package app.sanctuary.api.content.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import app.sanctuary.api.content.SaintContentRepository;
import app.sanctuary.api.content.SaintDateGroupResponse;
import app.sanctuary.api.content.SaintDetailResponse;
import app.sanctuary.api.content.SaintSummaryResponse;
import app.sanctuary.api.content.support.ContentNotFoundException;
import app.sanctuary.api.content.support.ContentRequestValidator;
import app.sanctuary.api.content.support.SupportedLanguage;

@Service
public class SaintContentService {

    private static final int MAX_DATE_RANGE_DAYS = 62;

    private final SaintContentRepository repository;
    private final ContentRequestValidator validator;

    public SaintContentService(SaintContentRepository repository, ContentRequestValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<SaintSummaryResponse> list(String language, String query) {
        return repository.list(SupportedLanguage.from(language), query);
    }

    public List<SaintSummaryResponse> getByFeastDay(int month, int day, String language) {
        validator.validateMonthDay(month, day);
        return repository.findByFeastDay(month, day, SupportedLanguage.from(language));
    }

    public List<SaintDateGroupResponse> getByDateRange(LocalDate start, LocalDate end, String language) {
        validator.validateDateRange(start, end, MAX_DATE_RANGE_DAYS);
        SupportedLanguage supportedLanguage = SupportedLanguage.from(language);

        List<SaintDateGroupResponse> response = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            response.add(new SaintDateGroupResponse(
                cursor,
                repository.findByFeastDay(cursor.getMonthValue(), cursor.getDayOfMonth(), supportedLanguage)
            ));
            cursor = cursor.plusDays(1);
        }
        return response;
    }

    public SaintDetailResponse getBySlug(String slug, String language) {
        return repository.findBySlug(slug, SupportedLanguage.from(language))
            .orElseThrow(() -> new ContentNotFoundException("No saint found for slug: " + slug));
    }
}
