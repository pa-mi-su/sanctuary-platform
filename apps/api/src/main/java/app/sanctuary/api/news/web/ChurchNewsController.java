package app.sanctuary.api.news.web;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import app.sanctuary.api.news.dto.ChurchNewsArticleDto;
import app.sanctuary.api.news.service.ChurchNewsService;

@RestController
@RequestMapping("/content/news")
public class ChurchNewsController {
    private final ChurchNewsService service;

    public ChurchNewsController(ChurchNewsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ChurchNewsArticleDto>> list(
        @RequestParam(defaultValue = "en") String lang,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(15)).cachePublic())
            .body(service.list(lang, limit));
    }
}
