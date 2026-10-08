package app.sanctuary.api.news.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ChurchNewsFeedParserTest {
    private final ChurchNewsFeedParser parser = new ChurchNewsFeedParser();

    @Test
    void parsesAndSanitizesApprovedRssItems() {
        String xml = """
            <?xml version="1.0"?><rss><channel><item>
              <title>A Church headline</title>
              <link>https://www.fides.org/en/news/123-example</link>
              <description>&lt;p&gt;A &lt;strong&gt;clear&lt;/strong&gt; summary.&lt;/p&gt;</description>
              <pubDate>Mon, 05 Oct 2026 04:30:00 -0400</pubDate>
            </item></channel></rss>
            """;

        var result = parser.parse(
            xml, "en", "Agenzia Fides", "www.fides.org", Set.of("www.fides.org"), "CC BY 4.0"
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("A Church headline");
        assertThat(result.getFirst().summary()).isEqualTo("A clear summary.");
        assertThat(result.getFirst().imageUrl()).isNull();
    }

    @Test
    void requiresTheExactLicensedArticleImage() {
        String xml = """
            <?xml version="1.0"?><rss><channel><item>
              <title>A Church headline</title>
              <link>https://www.fides.org/en/news/123-example</link>
              <description>A clear summary.</description>
              <pubDate>Mon, 05 Oct 2026 04:30:00 -0400</pubDate>
            </item></channel></rss>
            """;
        var article = parser.parse(
            xml, "en", "Agenzia Fides", "www.fides.org", Set.of("www.fides.org"), "CC BY 4.0"
        ).getFirst();
        String html = """
            <html><head><meta property="og:image"
              content="https://www.fides.org/app/webroot/files/appendeds/1/story.jpg"></head>
            <body><div class="thumbnail"><img
              src="https://www.fides.org/app/webroot/files/appendeds/1/story.jpg" alt="Bishop at Mass">
              <div class="caption">Photo: Agenzia Fides</div></div></body></html>
            """;

        var enriched = parser.requireLicensedImage(
            article, html, "www.fides.org", "CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/"
        );

        assertThat(enriched).isPresent();
        assertThat(enriched.orElseThrow().imageAlt()).isEqualTo("Bishop at Mass");
        assertThat(enriched.orElseThrow().imageCredit()).isEqualTo("Photo: Agenzia Fides");
    }

    @Test
    void excludesArticlesWithoutAnApprovedImage() {
        String xml = """
            <?xml version="1.0"?><rss><channel><item>
              <title>No image</title><link>https://www.fides.org/en/news/123-no-image</link>
            </item></channel></rss>
            """;
        var article = parser.parse(
            xml, "en", "Agenzia Fides", "www.fides.org", Set.of("www.fides.org"), "CC BY 4.0"
        ).getFirst();

        assertThat(parser.requireLicensedImage(
            article, "<html></html>", "www.fides.org", "CC BY 4.0", "https://creativecommons.org/licenses/by/4.0/"
        )).isEmpty();
    }

    @Test
    void rejectsLinksOutsideTheAllowlistedHost() {
        String xml = """
            <?xml version="1.0"?><rss><channel><item>
              <title>Untrusted</title><link>https://example.com/story</link>
            </item></channel></rss>
            """;

        assertThat(parser.parse(
            xml, "en", "Agenzia Fides", "www.fides.org", Set.of("www.fides.org"), "CC BY 4.0"
        )).isEmpty();
    }

    @Test
    void usesMediaRssImageAndCreditForLinkedPreview() {
        String xml = """
            <?xml version="1.0"?><rss xmlns:media="http://search.yahoo.com/mrss/"><channel><item>
              <title>A current EWTN story</title>
              <link>https://www.ewtnnews.com/vatican/current-story</link>
              <description>A concise publisher-supplied description.</description>
              <pubDate>Tue, 06 Oct 2026 18:20:00 GMT</pubDate>
              <media:content url="https://res.cloudinary.com/ewtn/image/upload/story.jpg"
                medium="image" type="image/jpeg" />
              <media:credit>Daniel Ibáñez / EWTN News</media:credit>
            </item></channel></rss>
            """;

        var article = parser.parse(
            xml, "en", "EWTN News", "www.ewtnnews.com", Set.of("res.cloudinary.com"), "Linked preview"
        ).getFirst();

        assertThat(article.imageUrl()).isEqualTo("https://res.cloudinary.com/ewtn/image/upload/story.jpg");
        assertThat(article.imageCredit()).isEqualTo("Daniel Ibáñez / EWTN News");
        assertThat(article.licenseName()).isEqualTo("Linked preview");
        assertThat(article.licenseUrl()).isEqualTo(article.canonicalUrl());
    }

    @Test
    void usesPublisherSuppliedHtmlThumbnailWithoutFollowingArticlePage() {
        String xml = """
            <?xml version="1.0"?><rss xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel><item>
              <title>Wiadomości z Kościoła</title>
              <link>https://ewtn.pl/aktualnosci/wiadomosc/</link>
              <description><![CDATA[
                <img src="https://ewtn.pl/wp-content/uploads/story.jpeg" />
                <p>Krótki opis wiadomości.</p>
              ]]></description>
            </item></channel></rss>
            """;

        var article = parser.parse(
            xml, "pl", "EWTN Polska", "ewtn.pl", Set.of("ewtn.pl"), "Linked preview"
        ).getFirst();

        assertThat(article.imageUrl()).isEqualTo("https://ewtn.pl/wp-content/uploads/story.jpeg");
        assertThat(article.summary()).isEqualTo("Krótki opis wiadomości.");
        assertThat(article.imageCredit()).isEqualTo("EWTN Polska");
    }

    @Test
    void rejectsFeedImagesOutsideTheSourceImageAllowlist() {
        String xml = """
            <?xml version="1.0"?><rss xmlns:media="http://search.yahoo.com/mrss/"><channel><item>
              <title>Valid story with an untrusted image</title>
              <link>https://www.ewtnnews.com/world/story</link>
              <media:content url="https://tracker.example/image.jpg" medium="image" />
            </item></channel></rss>
            """;

        var article = parser.parse(
            xml, "en", "EWTN News", "www.ewtnnews.com", Set.of("res.cloudinary.com"), "Linked preview"
        ).getFirst();

        assertThat(article.imageUrl()).isNull();
    }
}
