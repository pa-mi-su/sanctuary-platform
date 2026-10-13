package app.sanctuary.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.net.InetAddress;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class AuthAbuseProtectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthAbuseProtectionFilter.class);
    private static final AuthEndpoint REGISTER = new AuthEndpoint("/auth/register", "register");
    private static final AuthEndpoint LOGIN = new AuthEndpoint("/auth/login", "login");
    private static final AuthEndpoint NEWS_LOGIN = new AuthEndpoint("/auth/news/login", "news-login");
    private static final AuthEndpoint FORGOT_PASSWORD = new AuthEndpoint("/auth/forgot-password", "forgot-password");
    private static final AuthEndpoint RESEND_CONFIRMATION = new AuthEndpoint("/auth/resend-confirmation", "resend-confirmation");

    private final AuthAbuseProtectionProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, WindowCounter> attempts = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> newsAttempts = new ConcurrentHashMap<>();
    private final List<IpAddressMatcher> trustedProxies;

    public AuthAbuseProtectionFilter(AuthAbuseProtectionProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    AuthAbuseProtectionFilter(AuthAbuseProtectionProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.trustedProxies = properties.getTrustedProxyCidrs().stream().map(IpAddressMatcher::new).toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || endpointFor(request).isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = request instanceof ContentCachingRequestWrapper cached
            ? cached
            : new ContentCachingRequestWrapper(request, 4096);
        AuthEndpoint endpoint = endpointFor(wrappedRequest).orElseThrow();
        String ipAddress = clientIp(wrappedRequest);
        String rateKey = endpoint.name() + ":" + ipAddress;

        if (!allow(endpoint == NEWS_LOGIN ? newsAttempts : attempts, rateKey, limitFor(endpoint))) {
            log.warn("Auth rate limit exceeded endpoint={} ip={} userAgent={}", endpoint.name(), ipAddress, safeHeader(wrappedRequest, "User-Agent"));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many attempts. Please wait a moment and try again.\"}");
            return;
        }

        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            AuthRequestSummary summary = summarize(wrappedRequest);
            log.info(
                "Auth request endpoint={} status={} ip={} forwardedFor={} userAgent={} emailDomain={} emailHash={}",
                endpoint.name(),
                response.getStatus(),
                ipAddress,
                safeHeader(wrappedRequest, "X-Forwarded-For"),
                safeHeader(wrappedRequest, "User-Agent"),
                summary.emailDomain(),
                summary.emailHash()
            );
        }
    }

    private Optional<AuthEndpoint> endpointFor(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return Optional.empty();
        }

        String path = request.getRequestURI();
        if (REGISTER.path().equals(path)) {
            return Optional.of(REGISTER);
        }
        if (LOGIN.path().equals(path)) {
            return Optional.of(LOGIN);
        }
        if (NEWS_LOGIN.path().equals(path) || "/auth/news/verify".equals(path)) {
            return Optional.of(NEWS_LOGIN);
        }
        if (FORGOT_PASSWORD.path().equals(path)) {
            return Optional.of(FORGOT_PASSWORD);
        }
        if (RESEND_CONFIRMATION.path().equals(path)) {
            return Optional.of(RESEND_CONFIRMATION);
        }
        return Optional.empty();
    }

    private AuthAbuseProtectionProperties.Limit limitFor(AuthEndpoint endpoint) {
        if (endpoint == REGISTER) {
            return properties.register();
        }
        if (endpoint == LOGIN || endpoint == NEWS_LOGIN) {
            return properties.login();
        }
        if (endpoint == RESEND_CONFIRMATION) {
            return properties.resendConfirmation();
        }
        return properties.forgotPassword();
    }

    private synchronized boolean allow(Map<String, WindowCounter> attempts, String key, AuthAbuseProtectionProperties.Limit limit) {
        if (limit.maxAttempts() <= 0) {
            return false;
        }

        Instant now = clock.instant();
        attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        // Bound attacker-controlled key storage; fail closed when capacity is exhausted.
        if (!attempts.containsKey(key) && attempts.size() >= 10000) return false;
        WindowCounter counter = attempts.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.expiresAt())) {
                return new WindowCounter(1, now.plus(limit.window()));
            }
            return new WindowCounter(current.count() + 1, current.expiresAt());
        });
        return counter.count() <= limit.maxAttempts();
    }

    private String clientIp(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (trustedProxies.stream().noneMatch(proxy -> proxy.matches(peer))) return peer;
        // Sanctuary's ALB appends the observed client IP. Ignore every caller-supplied prefix.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.length() > 4096) return peer;
        String address = forwarded.substring(forwarded.lastIndexOf(',') + 1).trim();
        // Literal addresses only: never resolve attacker-controlled DNS names.
        if (address.isEmpty() || !address.matches("[0-9a-fA-F:.]+")) return peer;
        if (!address.contains(":")) {
            String[] octets = address.split("\\.", -1);
            if (octets.length != 4) return peer;
            for (String octet : octets) {
                if (!octet.matches("0|[1-9][0-9]{0,2}") || Integer.parseInt(octet) > 255) return peer;
            }
        }
        try {
            return InetAddress.getByName(address).getHostAddress();
        } catch (java.net.UnknownHostException exception) {
            return peer;
        }
    }

    private String safeHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.substring(0, Math.min(value.length(), 256)).replaceAll("[\\r\\n\\t]", " ").trim();
    }

    private AuthRequestSummary summarize(ContentCachingRequestWrapper request) {
        byte[] body = request.getContentAsByteArray();
        if (body.length == 0) {
            return AuthRequestSummary.empty();
        }

        try {
            JsonNode root = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            String email = root.path("email").asText("").trim().toLowerCase();
            if (email.isBlank() || !email.contains("@")) {
                return AuthRequestSummary.empty();
            }
            String domain = email.substring(email.lastIndexOf('@') + 1);
            return new AuthRequestSummary(domain, Integer.toHexString(email.hashCode()));
        } catch (Exception exception) {
            return AuthRequestSummary.empty();
        }
    }

    private record AuthEndpoint(String path, String name) {
    }

    private record WindowCounter(int count, Instant expiresAt) {
    }

    private record AuthRequestSummary(String emailDomain, String emailHash) {
        static AuthRequestSummary empty() {
            return new AuthRequestSummary("-", "-");
        }
    }
}
