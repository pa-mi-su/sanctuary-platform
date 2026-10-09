package app.sanctuary.api.news.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import app.sanctuary.api.news.dto.ChurchNewsArticleDto;

/** Parses publisher-operated RSS feeds without scraping article pages or copying article bodies. */
final class RssArticleFeedParser {
    private static final String CONTENT_NAMESPACE = "http://purl.org/rss/1.0/modules/content/";
    private static final String MEDIA_NAMESPACE = "http://search.yahoo.com/mrss/";

    List<ChurchNewsArticleDto> parse(String xml, SourceMetadata source) {
        try {
            var factory = secureDocumentBuilderFactory();
            var document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
            );
            NodeList items = document.getElementsByTagName("item");
            List<ChurchNewsArticleDto> articles = new ArrayList<>();
            for (int index = 0; index < items.getLength(); index++) {
                if (items.item(index) instanceof org.w3c.dom.Element item) {
                    parseItem(item, source).ifPresent(articles::add);
                }
            }
            return List.copyOf(articles);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not parse approved church news RSS feed", exception);
        }
    }

    private Optional<ChurchNewsArticleDto> parseItem(
        org.w3c.dom.Element item,
        SourceMetadata source
    ) {
        String title = normalizedText(text(item, "title"));
        String canonicalUrl = text(item, "link").trim();
        String description = text(item, "description");
        String encodedContent = namespacedText(item, CONTENT_NAMESPACE, "encoded");

        if (title.isBlank() || !isAllowedHttpsUrl(canonicalUrl, source.articleHosts())) {
            return Optional.empty();
        }

        OffsetDateTime publishedAt;
        try {
            publishedAt = OffsetDateTime.parse(text(item, "pubDate").trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (Exception ignored) {
            return Optional.empty();
        }

        Optional<ImageMetadata> image = imageMetadata(item, encodedContent, source);
        if (image.isEmpty()) {
            return Optional.empty();
        }

        ImageMetadata imageMetadata = image.get();
        return Optional.of(new ChurchNewsArticleDto(
            stableId(canonicalUrl),
            title,
            summarize(description),
            source.name(),
            canonicalUrl,
            imageMetadata.url(),
            imageMetadata.alt().isBlank() ? title : imageMetadata.alt(),
            imageMetadata.credit().isBlank() ? source.name() : imageMetadata.credit(),
            source.originalArticleLabel(),
            canonicalUrl,
            publishedAt,
            source.language()
        ));
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static Optional<ImageMetadata> imageMetadata(
        org.w3c.dom.Element item,
        String encodedContent,
        SourceMetadata source
    ) {
        String mediaUrl = firstAllowedAttribute(
            item.getElementsByTagNameNS(MEDIA_NAMESPACE, "content"), "url", source.imageHosts()
        );
        String enclosureUrl = firstAllowedImageEnclosure(item, source.imageHosts());
        Document content = Jsoup.parseBodyFragment(encodedContent == null ? "" : encodedContent);
        Element contentImage = content.selectFirst("img[src]");
        String contentImageUrl = contentImage == null ? "" : contentImage.attr("src").trim();

        String imageUrl = !mediaUrl.isBlank() ? mediaUrl
            : !enclosureUrl.isBlank() ? enclosureUrl
            : isAllowedHttpsUrl(contentImageUrl, source.imageHosts()) ? contentImageUrl : "";
        if (imageUrl.isBlank()) return Optional.empty();

        String mediaDescription = normalizedText(namespacedText(item, MEDIA_NAMESPACE, "description"));
        String contentAlt = contentImage == null ? "" : normalizedText(contentImage.attr("alt"));
        String alt = !mediaDescription.isBlank() ? mediaDescription : contentAlt;

        String mediaCredit = normalizedText(namespacedText(item, MEDIA_NAMESPACE, "credit"));
        String contentCredit = content.select("figcaption, .caption, span").stream()
            .map(element -> normalizedText(element.text()))
            .filter(RssArticleFeedParser::looksLikeCredit)
            .findFirst()
            .orElse("");
        String credit = mediaCredit.isBlank() ? contentCredit : mediaCredit;
        return Optional.of(new ImageMetadata(imageUrl, alt, credit));
    }

    private static String firstAllowedImageEnclosure(org.w3c.dom.Element item, Set<String> allowedHosts) {
        NodeList enclosures = item.getElementsByTagName("enclosure");
        for (int index = 0; index < enclosures.getLength(); index++) {
            if (!(enclosures.item(index) instanceof org.w3c.dom.Element enclosure)) continue;
            String type = enclosure.getAttribute("type").trim().toLowerCase(Locale.ROOT);
            String url = enclosure.getAttribute("url").trim();
            if (type.startsWith("image/") && isAllowedHttpsUrl(url, allowedHosts)) return url;
        }
        return "";
    }

    private static String firstAllowedAttribute(NodeList nodes, String attribute, Set<String> allowedHosts) {
        for (int index = 0; index < nodes.getLength(); index++) {
            if (!(nodes.item(index) instanceof org.w3c.dom.Element element)) continue;
            String medium = element.getAttribute("medium").trim();
            String type = element.getAttribute("type").trim();
            String url = element.getAttribute(attribute).trim();
            boolean isImage = "image".equalsIgnoreCase(medium) || type.toLowerCase(Locale.ROOT).startsWith("image/");
            if (isImage && isAllowedHttpsUrl(url, allowedHosts)) return url;
        }
        return "";
    }

    private static String text(org.w3c.dom.Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return "";
        Node node = nodes.item(0);
        return node == null ? "" : node.getTextContent();
    }

    private static String namespacedText(org.w3c.dom.Element parent, String namespace, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(namespace, localName);
        if (nodes.getLength() == 0) return "";
        Node node = nodes.item(0);
        return node == null ? "" : node.getTextContent();
    }

    private static String summarize(String html) {
        String value = normalizedText(Jsoup.parse(html == null ? "" : html).text());
        if (value.length() <= 280) return value;
        int boundary = value.lastIndexOf(' ', 277);
        return value.substring(0, boundary > 180 ? boundary : 277).trim() + "…";
    }

    private static boolean looksLikeCredit(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("credit") || normalized.contains("crédito") || normalized.contains("photo:");
    }

    private static String normalizedText(String value) {
        return (value == null ? "" : value)
            .replace('\u00a0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static boolean isAllowedHttpsUrl(String raw, Set<String> allowedHosts) {
        try {
            URI uri = URI.create(raw.trim());
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                && host != null
                && allowedHosts.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(host));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String stableId(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    record SourceMetadata(
        String language,
        String name,
        Set<String> articleHosts,
        Set<String> imageHosts,
        String originalArticleLabel
    ) {
        SourceMetadata {
            articleHosts = Set.copyOf(articleHosts);
            imageHosts = Set.copyOf(imageHosts);
        }
    }

    private record ImageMetadata(String url, String alt, String credit) {}
}
