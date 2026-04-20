package app.sanctuary.api.content.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import app.sanctuary.api.content.NovenaCalendarContentRepository;
import app.sanctuary.api.content.NovenaCalendarDateResponse;
import app.sanctuary.api.content.NovenaDetailResponse;
import app.sanctuary.api.content.NovenaSummaryResponse;
import app.sanctuary.api.content.support.ContentNotFoundException;
import app.sanctuary.api.content.support.ContentRequestValidator;
import app.sanctuary.api.content.support.SupportedLanguage;

@Service
public class NovenaContentService {

    private static final int MAX_DATE_RANGE_DAYS = 62;

    private final NovenaCalendarContentRepository repository;
    private final ContentRequestValidator validator;

    public NovenaContentService(NovenaCalendarContentRepository repository, ContentRequestValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    public List<NovenaSummaryResponse> list(String language, String query) {
        return repository.list(SupportedLanguage.from(language), query);
    }

    public List<NovenaSummaryResponse> listByIntentions(String language, String query) {
        return repository.listByIntentions(SupportedLanguage.from(language), query);
    }

    public List<NovenaCalendarDateResponse> getCalendarRange(LocalDate start, LocalDate end, String language) {
        validator.validateDateRange(start, end, MAX_DATE_RANGE_DAYS);
        return repository.calendarRange(start, end, SupportedLanguage.from(language));
    }

    public NovenaDetailResponse getBySlug(String slug, String language) {
        return repository.findBySlug(slug, SupportedLanguage.from(language))
            .orElseThrow(() -> new ContentNotFoundException("No novena found for slug: " + slug));
    }
}
