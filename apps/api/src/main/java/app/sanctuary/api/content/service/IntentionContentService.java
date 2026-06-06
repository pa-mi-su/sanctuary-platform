package app.sanctuary.api.content.service;

import org.springframework.stereotype.Service;

import app.sanctuary.api.content.dto.IntentionSearchResultDto;
import app.sanctuary.api.content.repository.IntentionContentRepository;
import app.sanctuary.api.content.support.SupportedLanguage;

@Service
public class IntentionContentService {

    private final IntentionContentRepository repository;

    public IntentionContentService(IntentionContentRepository repository) {
        this.repository = repository;
    }

    public IntentionSearchResultDto search(String language, String query) {
        return repository.search(SupportedLanguage.from(language), query);
    }
}
