package app.sanctuary.api.content.service;

import java.util.List;

import org.springframework.stereotype.Service;

import app.sanctuary.api.content.PrayerContentRepository;
import app.sanctuary.api.content.PrayerDetailResponse;
import app.sanctuary.api.content.PrayerSummaryResponse;
import app.sanctuary.api.content.support.ContentNotFoundException;
import app.sanctuary.api.content.support.SupportedLanguage;

@Service
public class PrayerContentService {

    private final PrayerContentRepository repository;

    public PrayerContentService(PrayerContentRepository repository) {
        this.repository = repository;
    }

    public List<PrayerSummaryResponse> list(String language, String query) {
        return repository.list(SupportedLanguage.from(language), query);
    }

    public PrayerDetailResponse getBySlug(String slug, String language) {
        return repository.findBySlug(slug, SupportedLanguage.from(language))
            .orElseThrow(() -> new ContentNotFoundException("No prayer found for slug: " + slug));
    }
}
