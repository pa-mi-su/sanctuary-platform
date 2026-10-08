package app.sanctuary.api.news.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class YouTubeNewsFeedParserTest {
    private final YouTubeNewsFeedParser parser = new YouTubeNewsFeedParser();
    private final YouTubeNewsFeedParser.SourceMetadata source =
        new YouTubeNewsFeedParser.SourceMetadata("en", "EWTN News");

    @Test
    void parsesOfficialVideoMetadataWithoutRewritingPublisherContent() {
        String xml = feed(entry(
            "news123",
            "A current Catholic news story | EWTN News Nightly",
            "https://www.youtube.com/watch?v=news123",
            "https://i2.ytimg.com/vi/news123/hqdefault.jpg",
            "A concise publisher-provided description about the story.\n\nSubscribe to EWTN News.",
            "2026-10-08T14:30:00+00:00"
        ));

        var result = parser.parse(xml, source);

        assertThat(result).hasSize(1);
        var article = result.getFirst();
        assertThat(article.id()).isEqualTo("news123");
        assertThat(article.title()).isEqualTo("A current Catholic news story | EWTN News Nightly");
        assertThat(article.summary()).isEqualTo("A concise publisher-provided description about the story.");
        assertThat(article.sourceName()).isEqualTo("EWTN News");
        assertThat(article.imageUrl()).isEqualTo("https://i2.ytimg.com/vi/news123/hqdefault.jpg");
        assertThat(article.imageCredit()).isEqualTo("EWTN News / YouTube");
        assertThat(article.licenseName()).isEqualTo("YouTube");
        assertThat(article.licenseUrl()).isEqualTo(article.canonicalUrl());
    }

    @Test
    void filtersLivestreamsFullProgramsAndShorts() {
        String xml = feed(
            entry("live1", "LIVE | Mass from the Vatican", "https://www.youtube.com/watch?v=live1",
                "https://i.ytimg.com/vi/live1/hqdefault.jpg", "Live coverage", "2026-10-08T10:00:00Z")
                + entry("show1", "EWTN News Nightly | Thursday, October 8", "https://www.youtube.com/watch?v=show1",
                "https://i.ytimg.com/vi/show1/hqdefault.jpg", "Full program", "2026-10-08T11:00:00Z")
                + entry("show2", "EWTN PLW | Full EPISODE | Wednesday", "https://www.youtube.com/watch?v=show2",
                "https://i.ytimg.com/vi/show2/hqdefault.jpg", "Full program", "2026-10-08T11:15:00Z")
                + entry("live2", "EN VIVO | Audiencia General", "https://www.youtube.com/watch?v=live2",
                "https://i.ytimg.com/vi/live2/hqdefault.jpg", "Transmision en vivo", "2026-10-08T11:30:00Z")
                + entry("show3", "EWTN Noticias | Miércoles | Programa completo", "https://www.youtube.com/watch?v=show3",
                "https://i.ytimg.com/vi/show3/hqdefault.jpg", "Programa completo", "2026-10-08T11:45:00Z")
                + entry("short1", "A quotation without a news report", "https://www.youtube.com/shorts/short1",
                "https://i.ytimg.com/vi/short1/hqdefault.jpg", "Short", "2026-10-08T12:00:00Z")
        );

        assertThat(parser.parse(xml, source)).isEmpty();
    }

    @Test
    void rejectsMismatchedVideoLinksAndThumbnailHosts() {
        String xml = feed(
            entry("video1", "Wrong link", "https://example.com/watch?v=video1",
                "https://i.ytimg.com/vi/video1/hqdefault.jpg", "Story", "2026-10-08T10:00:00Z")
                + entry("video2", "Wrong image", "https://www.youtube.com/watch?v=video2",
                "https://tracker.example/vi/video2/hqdefault.jpg", "Story", "2026-10-08T11:00:00Z")
                + entry("video3", "Mismatched image", "https://www.youtube.com/watch?v=video3",
                "https://i.ytimg.com/vi/different/hqdefault.jpg", "Story", "2026-10-08T12:00:00Z")
        );

        assertThat(parser.parse(xml, source)).isEmpty();
    }

    @Test
    void rejectsEntriesWithoutAValidPublicationDate() {
        String xml = feed(entry(
            "video1", "A story", "https://www.youtube.com/watch?v=video1",
            "https://i.ytimg.com/vi/video1/hqdefault.jpg", "Story", "not-a-date"
        ));

        assertThat(parser.parse(xml, source)).isEmpty();
    }

    private static String feed(String entries) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom"
                  xmlns:yt="http://www.youtube.com/xml/schemas/2015"
                  xmlns:media="http://search.yahoo.com/mrss/">
              %s
            </feed>
            """.formatted(entries);
    }

    private static String entry(
        String id,
        String title,
        String link,
        String thumbnail,
        String description,
        String published
    ) {
        return """
            <entry>
              <yt:videoId>%s</yt:videoId>
              <title>%s</title>
              <link rel="alternate" href="%s" />
              <published>%s</published>
              <media:group>
                <media:thumbnail url="%s" width="480" height="360" />
                <media:description><![CDATA[%s]]></media:description>
              </media:group>
            </entry>
            """.formatted(id, title, link, published, thumbnail, description);
    }
}
