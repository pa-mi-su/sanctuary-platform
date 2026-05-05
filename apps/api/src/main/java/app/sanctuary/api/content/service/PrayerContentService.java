package app.sanctuary.api.content.service;

import java.util.List;

import org.springframework.stereotype.Service;

import app.sanctuary.api.content.dto.PrayerDetailDto;
import app.sanctuary.api.content.dto.PrayerSummaryDto;
import app.sanctuary.api.content.repository.PrayerContentRepository;
import app.sanctuary.api.content.support.ContentNotFoundException;
import app.sanctuary.api.content.support.SupportedLanguage;

@Service
public class PrayerContentService {

    private final PrayerContentRepository repository;

    public PrayerContentService(PrayerContentRepository repository) {
        this.repository = repository;
    }

    public List<PrayerSummaryDto> list(String language, String query, String category, String excludeCategory) {
        return repository.list(SupportedLanguage.from(language), query, category, excludeCategory);
    }

    public PrayerDetailDto getBySlug(String slug, String language) {
        return repository.findBySlug(slug, SupportedLanguage.from(language))
            .orElseThrow(() -> new ContentNotFoundException("No prayer found for slug: " + slug));
    }
}
