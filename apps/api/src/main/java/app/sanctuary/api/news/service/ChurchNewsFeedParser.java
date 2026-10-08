package app.sanctuary.api.news.service;

import java.io.ByteArrayInputStream;
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
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import app.sanctuary.api.news.dto.ChurchNewsArticleDto;

final class ChurchNewsFeedParser {

    List<ChurchNewsArticleDto> parse(
        String xml,
        String language,
        String sourceName,
        String allowedArticleHost,
        Set<String> allowedImageHosts,
        String previewLabel
    ) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            var document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
            );
            NodeList items = document.getElementsByTagName("item");
            List<ChurchNewsArticleDto> articles = new ArrayList<>();
            for (int index = 0; index < items.getLength(); index++) {
                Element item = (Element) items.item(index);
                String title = text(item, "title");
                String link = text(item, "link");
                if (title.isBlank() || !isAllowedHttpsUrl(link, Set.of(allowedArticleHost))) {
                    continue;
                }
                String summary = summarize(text(item, "description"));
                OffsetDateTime publishedAt = parseDate(text(item, "pubDate"));
                String imageUrl = feedImageUrl(item, allowedImageHosts).orElse(null);
                String imageCredit = imageUrl == null ? null : feedImageCredit(item, sourceName);
                articles.add(new ChurchNewsArticleDto(
                    stableId(link), title.trim(), summary, sourceName, link.trim(), imageUrl,
                    imageUrl == null ? null : title.trim(), imageCredit,
                    imageUrl == null ? null : previewLabel,
                    imageUrl == null ? null : link.trim(),
                    publishedAt, language
                ));
            }
            return List.copyOf(articles);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not parse approved church news feed", exception);
        }
    }

    Optional<ChurchNewsArticleDto> requireLicensedImage(
        ChurchNewsArticleDto article,
        String pageHtml,
        String allowedHost,
        String licenseName,
        String licenseUrl
    ) {
        var page = Jsoup.parse(pageHtml, article.canonicalUrl());
        String imageUrl = page.select("meta[property=og:image]").attr("abs:content").trim();
        if (!isAllowedHttpsUrl(imageUrl, Set.of(allowedHost))) {
            return Optional.empty();
        }

        var image = page.selectFirst("img[src=\"" + imageUrl + "\"]");
        String imageAlt = image == null ? "" : image.attr("alt").trim();
        String imageCredit = "";
        if (image != null) {
            var thumbnail = image.closest(".thumbnail");
            if (thumbnail != null) {
                imageCredit = thumbnail.select(".caption, p").text().replaceAll("\\s+", " ").trim();
            }
        }
        if (imageAlt.isBlank()) imageAlt = article.title();
        if (imageCredit.isBlank()) imageCredit = article.sourceName();

        return Optional.of(new ChurchNewsArticleDto(
            article.id(), article.title(), article.summary(), article.sourceName(), article.canonicalUrl(),
            imageUrl, imageAlt, imageCredit, licenseName, licenseUrl,
            article.publishedAt(), article.language()
        ));
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }

    private static Optional<String> feedImageUrl(Element item, Set<String> allowedImageHosts) {
        for (String tag : List.of("media:content", "enclosure")) {
            NodeList nodes = item.getElementsByTagName(tag);
            for (int index = 0; index < nodes.getLength(); index++) {
                if (!(nodes.item(index) instanceof Element element)) continue;
                String candidate = element.getAttribute("url").trim();
                String type = element.getAttribute("type").trim().toLowerCase(Locale.ROOT);
                String medium = element.getAttribute("medium").trim().toLowerCase(Locale.ROOT);
                boolean isImage = type.isBlank() || type.startsWith("image/") || "image".equals(medium);
                if (isImage && isAllowedHttpsUrl(candidate, allowedImageHosts)) {
                    return Optional.of(candidate);
                }
            }
        }

        for (String tag : List.of("description", "content:encoded")) {
            String html = text(item, tag);
            if (html.isBlank()) continue;
            for (var image : Jsoup.parseBodyFragment(html).select("img[src]")) {
                String candidate = image.attr("src").trim();
                if (isAllowedHttpsUrl(candidate, allowedImageHosts)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private static String feedImageCredit(Element item, String fallback) {
        String mediaCredit = text(item, "media:credit").replaceAll("\\s+", " ").trim();
        if (!mediaCredit.isBlank()) return mediaCredit;

        for (String tag : List.of("content:encoded", "description")) {
            String html = text(item, tag);
            if (html.isBlank()) continue;
            var page = Jsoup.parseBodyFragment(html);
            String credit = page.select("figcaption, .caption, span").stream()
                .map(element -> element.text().replaceAll("\\s+", " ").trim())
                .filter(value -> !value.isBlank())
                .filter(value -> value.toLowerCase(Locale.ROOT).contains("credit")
                    || value.toLowerCase(Locale.ROOT).contains("crédito")
                    || value.toLowerCase(Locale.ROOT).contains("photo"))
                .findFirst()
                .orElse("");
            if (!credit.isBlank()) return credit;
        }
        return fallback;
    }

    private static String summarize(String html) {
        String text = Jsoup.parse(html == null ? "" : html).text()
            .replace('\u00a0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
        if (text.length() <= 280) return text;
        int boundary = text.lastIndexOf(' ', 277);
        return text.substring(0, boundary > 180 ? boundary : 277).trim() + "…";
    }

    private static OffsetDateTime parseDate(String raw) {
        try {
            return OffsetDateTime.parse(raw.trim(), DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (Exception ignored) {
            return OffsetDateTime.now();
        }
    }

    private static boolean isAllowedHttpsUrl(String raw, Set<String> allowedHosts) {
        try {
            var uri = java.net.URI.create(raw.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                && allowedHosts.stream().anyMatch(host -> host.equalsIgnoreCase(uri.getHost()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String stableId(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
            .digest(value.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest, 0, 16);
    }
}
