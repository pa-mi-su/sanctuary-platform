package app.sanctuary.api.news.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.sanctuary.api.news.dto.ChurchNewsArticleDto;

@Service
public class ChurchNewsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChurchNewsService.class);
    private static final String FIDES_LICENSE_NAME = "CC BY 4.0";
    private static final String FIDES_LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/";
    private static final Source FIDES_ENGLISH = new Source(
        "en", "Agenzia Fides", "https://www.fides.org/en/news/rss", "www.fides.org",
        Set.of("www.fides.org"), FIDES_LICENSE_NAME, true
    );
    private static final Source FIDES_SPANISH = new Source(
        "es", "Agencia Fides", "https://www.fides.org/es/news/rss", "www.fides.org",
        Set.of("www.fides.org"), FIDES_LICENSE_NAME, true
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(8))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
    private final ChurchNewsFeedParser parser = new ChurchNewsFeedParser();
    private final Map<String, List<ChurchNewsArticleDto>> cache = new ConcurrentHashMap<>();

    public List<ChurchNewsArticleDto> list(String requestedLanguage, int requestedLimit) {
        String language = normalizeLanguage(requestedLanguage);
        int limit = Math.max(1, Math.min(requestedLimit, 30));
        List<ChurchNewsArticleDto> articles = cache.get(language);
        if (articles == null) {
            articles = refresh(sourceFor(language));
            cache.put(language, articles);
        }
        if (articles.isEmpty()) {
            Source fallback = "es".equals(language) ? FIDES_SPANISH : FIDES_ENGLISH;
            articles = refresh(fallback);
            if (!articles.isEmpty()) cache.put(language, articles);
        }
        if (articles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Church news is temporarily unavailable");
        }
        return articles.stream().limit(limit).toList();
    }

    @Scheduled(fixedDelayString = "${sanctuary.news.refresh-ms:600000}")
    void scheduledRefresh() {
        refreshAndCache(FIDES_ENGLISH);
        refreshAndCache(FIDES_SPANISH);
    }

    private List<ChurchNewsArticleDto> refresh(Source source) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(source.url()))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/rss+xml, application/xml;q=0.9")
                .header("User-Agent", "Sanctuary/1.0 (+https://mydailysanctuary.com)")
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Approved church news source {} returned HTTP {}", source.name(), response.statusCode());
                return cache.getOrDefault(source.language(), List.of());
            }
            List<ChurchNewsArticleDto> candidates = parser.parse(
                response.body(), source.language(), source.name(), source.allowedArticleHost(),
                source.allowedImageHosts(), source.previewLabel()
            ).stream().limit(20).toList();
            if (!source.resolveImageFromArticle()) {
                return candidates.stream()
                    .filter(article -> article.imageUrl() != null)
                    .limit(12)
                    .toList();
            }
            List<CompletableFuture<ChurchNewsArticleDto>> imageRequests = candidates.stream()
                .map(article -> fetchLicensedArticleImage(article, source))
                .toList();
            return imageRequests.stream()
                .map(CompletableFuture::join)
                .filter(java.util.Objects::nonNull)
                .toList();
        } catch (Exception exception) {
            LOGGER.warn("Could not refresh approved church news source {}", source.name(), exception);
            return cache.getOrDefault(source.language(), List.of());
        }
    }

    private CompletableFuture<ChurchNewsArticleDto> fetchLicensedArticleImage(
        ChurchNewsArticleDto article,
        Source source
    ) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(article.canonicalUrl()))
            .timeout(Duration.ofSeconds(12))
            .header("Accept", "text/html")
            .header("User-Agent", "Sanctuary/1.0 (+https://mydailysanctuary.com)")
            .GET()
            .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
                return parser.requireLicensedImage(
                    article,
                    response.body(),
                    source.allowedArticleHost(),
                    FIDES_LICENSE_NAME,
                    FIDES_LICENSE_URL
                ).orElse(null);
            })
            .exceptionally(exception -> {
                LOGGER.debug("Could not resolve licensed image for {}", article.canonicalUrl(), exception);
                return null;
            });
    }

    private void refreshAndCache(Source source) {
        List<ChurchNewsArticleDto> refreshed = refresh(source);
        if (!refreshed.isEmpty()) cache.put(source.language(), refreshed);
    }

    private Source sourceFor(String language) {
        return switch (language) {
            case "es" -> FIDES_SPANISH;
            default -> FIDES_ENGLISH;
        };
    }

    private String normalizeLanguage(String raw) {
        if (raw == null) return "en";
        return switch (raw.trim().toLowerCase()) {
            case "es" -> "es";
            case "pl" -> "pl";
            default -> "en";
        };
    }

    private record Source(
        String language,
        String name,
        String url,
        String allowedArticleHost,
        Set<String> allowedImageHosts,
        String previewLabel,
        boolean resolveImageFromArticle
    ) {}
}
