package app.sanctuary.api.content.service;

import java.util.List;

import org.springframework.stereotype.Service;

import app.sanctuary.api.content.dto.SaintSummaryDto;
import app.sanctuary.api.content.dto.SearchTermDto;
import app.sanctuary.api.content.repository.PatronageContentRepository;
import app.sanctuary.api.content.support.SupportedLanguage;

@Service
public class PatronageContentService {

    private final PatronageContentRepository repository;

    public PatronageContentService(PatronageContentRepository repository) {
        this.repository = repository;
    }

    public List<SearchTermDto> searchTerms(String language, String query) {
        return repository.searchTerms(SupportedLanguage.from(language), query);
    }

    public List<SaintSummaryDto> getSaintsByPatronage(String language, String key) {
        return repository.findSaintsByPatronage(SupportedLanguage.from(language), key);
    }
}
