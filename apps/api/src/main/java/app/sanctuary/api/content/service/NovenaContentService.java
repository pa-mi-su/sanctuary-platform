package app.sanctuary.api.content.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import app.sanctuary.api.content.dto.NovenaCalendarDateDto;
import app.sanctuary.api.content.dto.NovenaDetailDto;
import app.sanctuary.api.content.dto.NovenaSummaryDto;
import app.sanctuary.api.content.repository.NovenaCalendarContentRepository;
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

    public List<NovenaSummaryDto> list(String language, String query) {
        return repository.list(SupportedLanguage.from(language), query);
    }

    public List<NovenaSummaryDto> listByIntentions(String language, String query) {
        return repository.listByIntentions(SupportedLanguage.from(language), query);
    }

    public List<NovenaCalendarDateDto> getCalendarRange(LocalDate start, LocalDate end, String language) {
        validator.validateDateRange(start, end, MAX_DATE_RANGE_DAYS);
        return repository.calendarRange(start, end, SupportedLanguage.from(language));
    }

    public NovenaDetailDto getBySlug(String slug, String language) {
        return repository.findBySlug(slug, SupportedLanguage.from(language))
            .orElseThrow(() -> new ContentNotFoundException("No novena found for slug: " + slug));
    }
}
