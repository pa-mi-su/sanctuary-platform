package app.sanctuary.api.news.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import app.sanctuary.api.news.dto.ChurchNewsArticleDto;

@Service
public class ChurchNewsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChurchNewsService.class);
    private static final Source EWTN_NEWS = new Source(
        "en",
        "EWTN News",
        URI.create("https://www.ewtnnews.com/rss"),
        Set.of("www.ewtnnews.com"),
        Set.of("res.cloudinary.com"),
        "Original article"
    );
    private static final Source ACI_PRENSA = new Source(
        "es",
        "ACI Prensa",
        URI.create("https://www.aciprensa.com/rss/noticias.xml"),
        Set.of("www.aciprensa.com"),
        Set.of("res.cloudinary.com"),
        "Artículo original"
    );

    private final HttpClient httpClient;
    private final RssArticleFeedParser parser;
    private final Map<String, List<ChurchNewsArticleDto>> cache = new ConcurrentHashMap<>();

    public ChurchNewsService() {
        this(
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_1_1)
                .build(),
            new RssArticleFeedParser()
        );
    }

    ChurchNewsService(HttpClient httpClient, RssArticleFeedParser parser) {
        this.httpClient = httpClient;
        this.parser = parser;
    }

    public List<ChurchNewsArticleDto> list(String requestedLanguage, int requestedLimit) {
        String language = normalizeLanguage(requestedLanguage);
        int limit = Math.max(1, Math.min(requestedLimit, 30));
        List<ChurchNewsArticleDto> articles = cache.get(language);
        if (articles == null) {
            articles = refresh(sourceFor(language), language);
            if (!articles.isEmpty()) cache.put(language, articles);
        }
        if (articles.isEmpty() && !"en".equals(language)) {
            articles = cache.computeIfAbsent("en", key -> refresh(EWTN_NEWS, key));
            if (!articles.isEmpty()) cache.put(language, articles);
        }
        if (articles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Church news is temporarily unavailable");
        }
        return articles.stream().limit(limit).toList();
    }

    @Scheduled(fixedDelayString = "${sanctuary.news.refresh-ms:900000}")
    void scheduledRefresh() {
        refreshAndCache("en", EWTN_NEWS);
        refreshAndCache("es", ACI_PRENSA);
        List<ChurchNewsArticleDto> english = cache.getOrDefault("en", List.of());
        if (!english.isEmpty()) cache.put("pl", english);
    }

    private List<ChurchNewsArticleDto> refresh(Source source, String cacheKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder(source.feedUrl())
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/rss+xml, application/xml;q=0.9")
                .header("User-Agent", "Sanctuary/1.0 (+https://mydailysanctuary.com)")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Official news feed {} returned HTTP {}", source.name(), response.statusCode());
                return cache.getOrDefault(cacheKey, List.of());
            }
            List<ChurchNewsArticleDto> parsed = parser.parse(
                response.body(),
                new RssArticleFeedParser.SourceMetadata(
                    source.language(),
                    source.name(),
                    source.articleHosts(),
                    source.imageHosts(),
                    source.originalArticleLabel()
                )
            );
            Map<String, ChurchNewsArticleDto> uniqueStories = new LinkedHashMap<>();
            for (ChurchNewsArticleDto article : parsed) {
                uniqueStories.putIfAbsent(normalizedTitle(article.title()), article);
            }
            return uniqueStories.values().stream().limit(12).toList();
        } catch (Exception exception) {
            LOGGER.warn("Could not refresh official news feed {}", source.name(), exception);
            return cache.getOrDefault(cacheKey, List.of());
        }
    }

    private void refreshAndCache(String cacheKey, Source source) {
        List<ChurchNewsArticleDto> refreshed = refresh(source, cacheKey);
        if (!refreshed.isEmpty()) cache.put(cacheKey, refreshed);
    }

    private Source sourceFor(String language) {
        return "es".equals(language) ? ACI_PRENSA : EWTN_NEWS;
    }

    private String normalizeLanguage(String raw) {
        if (raw == null) return "en";
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "es" -> "es";
            case "pl" -> "pl";
            default -> "en";
        };
    }

    private static String normalizedTitle(String value) {
        return value.toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}]+", " ")
            .trim();
    }

    private record Source(
        String language,
        String name,
        URI feedUrl,
        Set<String> articleHosts,
        Set<String> imageHosts,
        String originalArticleLabel
    ) {}
}
