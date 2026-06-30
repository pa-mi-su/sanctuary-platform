package app.sanctuary.api.content.service;

import org.springframework.stereotype.Service;

import app.sanctuary.api.content.dto.IntentionSearchResultDto;
import app.sanctuary.api.content.dto.NovenaSummaryDto;
import app.sanctuary.api.content.dto.SearchTermDto;
import app.sanctuary.api.content.repository.IntentionContentRepository;
import app.sanctuary.api.content.support.SupportedLanguage;
import java.util.List;

@Service
public class IntentionContentService {

    private final IntentionContentRepository repository;

    public IntentionContentService(IntentionContentRepository repository) {
        this.repository = repository;
    }

    public IntentionSearchResultDto search(String language, String query) {
        return repository.search(SupportedLanguage.from(language), query);
    }

    public List<SearchTermDto> searchNovenaIntentionTerms(String language, String query) {
        return repository.searchNovenaIntentionTerms(SupportedLanguage.from(language), query);
    }

    public List<NovenaSummaryDto> getNovenasByIntention(String language, String key) {
        return repository.findNovenasByIntention(SupportedLanguage.from(language).code(), key);
    }
}
