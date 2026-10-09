package app.sanctuary.api.news.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class RssArticleFeedParserTest {
    private final RssArticleFeedParser parser = new RssArticleFeedParser();
    private final RssArticleFeedParser.SourceMetadata ewtn = new RssArticleFeedParser.SourceMetadata(
        "en", "EWTN News", Set.of("www.ewtnnews.com"), Set.of("res.cloudinary.com"), "Original article"
    );

    @Test
    void parsesPublisherArticleMetadataAndMediaRssCredit() {
        String xml = feed(item(
            "A current Catholic news story",
            "https://www.ewtnnews.com/vatican/current-story",
            "A concise <strong>publisher-provided</strong> description.",
            "Thu, 08 Oct 2026 17:13:28 GMT",
            """
            <enclosure url="https://res.cloudinary.com/ewtn/image/upload/story.webp" type="image/webp" />
            <media:content url="https://res.cloudinary.com/ewtn/image/upload/story.webp" medium="image" type="image/webp">
              <media:description>Related news photograph</media:description>
              <media:credit>Vatican Media</media:credit>
            </media:content>
            """,
            ""
        ));

        var result = parser.parse(xml, ewtn);

        assertThat(result).hasSize(1);
        var article = result.getFirst();
        assertThat(article.title()).isEqualTo("A current Catholic news story");
        assertThat(article.summary()).isEqualTo("A concise publisher-provided description.");
        assertThat(article.canonicalUrl()).isEqualTo("https://www.ewtnnews.com/vatican/current-story");
        assertThat(article.imageUrl()).isEqualTo("https://res.cloudinary.com/ewtn/image/upload/story.webp");
        assertThat(article.imageAlt()).isEqualTo("Related news photograph");
        assertThat(article.imageCredit()).isEqualTo("Vatican Media");
        assertThat(article.licenseName()).isEqualTo("Original article");
        assertThat(article.licenseUrl()).isEqualTo(article.canonicalUrl());
    }

    @Test
    void extractsSpanishFeedImageAndCreditFromPublisherHtml() {
        var aci = new RssArticleFeedParser.SourceMetadata(
            "es", "ACI Prensa", Set.of("www.aciprensa.com"), Set.of("res.cloudinary.com"), "Artículo original"
        );
        String content = """
            <content:encoded><![CDATA[
              <div><img src="https://res.cloudinary.com/ewtn/image/upload/story.jpg?w=800&amp;jpg"
                   alt="El Papa durante la audiencia" />
              <span>Crédito: Vatican Media.</span></div>
            ]]></content:encoded>
            """;
        String xml = feed(item(
            "Una noticia católica", "https://www.aciprensa.com/noticias/123/noticia",
            "Resumen publicado por ACI Prensa.", "Thu, 08 Oct 2026 10:15:31 -0500", "", content
        ));

        var article = parser.parse(xml, aci).getFirst();

        assertThat(article.imageUrl()).startsWith("https://res.cloudinary.com/ewtn/image/upload/story.jpg");
        assertThat(article.imageAlt()).isEqualTo("El Papa durante la audiencia");
        assertThat(article.imageCredit()).isEqualTo("Crédito: Vatican Media.");
        assertThat(article.language()).isEqualTo("es");
    }

    @Test
    void requiresRelatedImageAndValidPublicationDate() {
        String withoutImage = item(
            "No image", "https://www.ewtnnews.com/world/no-image", "Summary",
            "Thu, 08 Oct 2026 17:13:28 GMT", "", ""
        );
        String invalidDate = item(
            "Bad date", "https://www.ewtnnews.com/world/bad-date", "Summary", "not-a-date",
            "<enclosure url=\"https://res.cloudinary.com/ewtn/image/upload/story.jpg\" type=\"image/jpeg\" />", ""
        );

        assertThat(parser.parse(feed(withoutImage + invalidDate), ewtn)).isEmpty();
    }

    @Test
    void rejectsUnapprovedArticleAndImageHosts() {
        String badArticle = item(
            "Bad article", "https://example.com/copied-story", "Summary", "Thu, 08 Oct 2026 17:13:28 GMT",
            "<enclosure url=\"https://res.cloudinary.com/ewtn/image/upload/story.jpg\" type=\"image/jpeg\" />", ""
        );
        String badImage = item(
            "Bad image", "https://www.ewtnnews.com/world/bad-image", "Summary", "Thu, 08 Oct 2026 17:13:28 GMT",
            "<enclosure url=\"https://tracker.example/story.jpg\" type=\"image/jpeg\" />", ""
        );

        assertThat(parser.parse(feed(badArticle + badImage), ewtn)).isEmpty();
    }

    private static String feed(String items) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"
                 xmlns:content="http://purl.org/rss/1.0/modules/content/"
                 xmlns:media="http://search.yahoo.com/mrss/">
              <channel>%s</channel>
            </rss>
            """.formatted(items);
    }

    private static String item(
        String title,
        String link,
        String description,
        String published,
        String imageElements,
        String encodedContent
    ) {
        return """
            <item>
              <title><![CDATA[%s]]></title>
              <link>%s</link>
              <description><![CDATA[%s]]></description>
              <pubDate>%s</pubDate>
              %s
              %s
            </item>
            """.formatted(title, link, description, published, imageElements, encodedContent);
    }
}
