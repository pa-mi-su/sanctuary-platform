package app.sanctuary.api.news.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import app.sanctuary.api.news.dto.ChurchNewsArticleDto;

/** Parses the public Atom/Media RSS feed published for an official YouTube channel. */
final class YouTubeNewsFeedParser {
    private static final Set<String> ALLOWED_VIDEO_HOSTS = Set.of("www.youtube.com", "youtube.com");
    private static final Set<String> BLOCKED_TITLE_PREFIXES = Set.of(
        "live |", "live:", "live from", "en vivo |", "en vivo:",
        "trailer |", "promo |", "ewtn news nightly |", "the world over with"
    );
    private static final Set<String> BLOCKED_TITLE_PHRASES = Set.of(
        "24/7 live", "livestream", "full episode", "programa completo"
    );

    List<ChurchNewsArticleDto> parse(String xml, SourceMetadata source) {
        try {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            var document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
            );
            NodeList entries = document.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
            List<ChurchNewsArticleDto> articles = new ArrayList<>();
            for (int index = 0; index < entries.getLength(); index++) {
                Element entry = (Element) entries.item(index);
                parseEntry(entry, source).ifPresent(articles::add);
            }
            return List.copyOf(articles);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not parse official YouTube news feed", exception);
        }
    }

    private java.util.Optional<ChurchNewsArticleDto> parseEntry(Element entry, SourceMetadata source) {
        String videoId = text(entry, "http://www.youtube.com/xml/schemas/2015", "videoId").trim();
        String title = text(entry, "http://www.w3.org/2005/Atom", "title").replaceAll("\\s+", " ").trim();
        String canonicalUrl = alternateLink(entry);
        String thumbnailUrl = thumbnailUrl(entry);
        String description = text(entry, "http://search.yahoo.com/mrss/", "description");

        if (videoId.isBlank() || title.isBlank() || shouldExclude(title, canonicalUrl)) {
            return java.util.Optional.empty();
        }
        if (!isAllowedVideoUrl(canonicalUrl, videoId) || !isAllowedThumbnailUrl(thumbnailUrl, videoId)) {
            return java.util.Optional.empty();
        }

        OffsetDateTime publishedAt;
        try {
            publishedAt = OffsetDateTime.parse(text(entry, "http://www.w3.org/2005/Atom", "published").trim());
        } catch (Exception ignored) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new ChurchNewsArticleDto(
            videoId,
            title,
            summarizePublisherDescription(description),
            source.name(),
            canonicalUrl,
            thumbnailUrl,
            title,
            source.name() + " / YouTube",
            "YouTube",
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

    private static String alternateLink(Element entry) {
        NodeList links = entry.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "link");
        for (int index = 0; index < links.getLength(); index++) {
            if (!(links.item(index) instanceof Element link)) continue;
            if ("alternate".equals(link.getAttribute("rel"))) return link.getAttribute("href").trim();
        }
        return "";
    }

    private static String thumbnailUrl(Element entry) {
        NodeList thumbnails = entry.getElementsByTagNameNS("http://search.yahoo.com/mrss/", "thumbnail");
        if (thumbnails.getLength() == 0 || !(thumbnails.item(0) instanceof Element thumbnail)) return "";
        return thumbnail.getAttribute("url").trim();
    }

    private static String text(Element parent, String namespace, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS(namespace, localName);
        if (nodes.getLength() == 0) return "";
        Node node = nodes.item(0);
        return node == null ? "" : node.getTextContent();
    }

    private static boolean shouldExclude(String title, String canonicalUrl) {
        String normalizedTitle = title.toLowerCase(Locale.ROOT);
        return canonicalUrl.contains("/shorts/")
            || BLOCKED_TITLE_PREFIXES.stream().anyMatch(normalizedTitle::startsWith)
            || BLOCKED_TITLE_PHRASES.stream().anyMatch(normalizedTitle::contains);
    }

    private static boolean isAllowedVideoUrl(String raw, String expectedVideoId) {
        try {
            URI uri = URI.create(raw);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !ALLOWED_VIDEO_HOSTS.contains(uri.getHost())) return false;
            return "/watch".equals(uri.getPath()) && queryParameter(uri.getRawQuery(), "v").equals(expectedVideoId);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isAllowedThumbnailUrl(String raw, String expectedVideoId) {
        try {
            URI uri = URI.create(raw);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            boolean allowedHost = host.equals("i.ytimg.com")
                || host.equals("img.youtube.com")
                || host.matches("i[0-9]+\\.ytimg\\.com");
            return "https".equalsIgnoreCase(uri.getScheme())
                && allowedHost
                && uri.getPath() != null
                && uri.getPath().contains("/vi/" + expectedVideoId + "/");
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String queryParameter(String rawQuery, String name) {
        if (rawQuery == null) return "";
        for (String pair : rawQuery.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(name)) return parts[1];
        }
        return "";
    }

    private static String summarizePublisherDescription(String raw) {
        String plainText = Jsoup.parse(raw == null ? "" : raw).text()
            .replace('\u00a0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
        if (plainText.isBlank()) return "";

        int boilerplateStart = firstPositiveIndex(
            plainText,
            " Subscribe ", " Follow ", " Sign up ", " --------", " 📩 ", " 👥 ", " 💻 "
        );
        String editorialText = boilerplateStart < 0 ? plainText : plainText.substring(0, boilerplateStart).trim();
        if (editorialText.length() <= 280) return editorialText;
        int boundary = editorialText.lastIndexOf(' ', 277);
        return editorialText.substring(0, boundary > 180 ? boundary : 277).trim() + "…";
    }

    private static int firstPositiveIndex(String value, String... markers) {
        int result = -1;
        for (String marker : markers) {
            int index = value.indexOf(marker);
            if (index >= 0 && (result < 0 || index < result)) result = index;
        }
        return result;
    }

    record SourceMetadata(String language, String name) {}
}
