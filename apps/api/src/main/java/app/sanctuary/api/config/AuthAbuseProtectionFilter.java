package app.sanctuary.api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class AuthAbuseProtectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthAbuseProtectionFilter.class);
    private static final AuthEndpoint REGISTER = new AuthEndpoint("/auth/register", "register");
    private static final AuthEndpoint LOGIN = new AuthEndpoint("/auth/login", "login");
    private static final AuthEndpoint FORGOT_PASSWORD = new AuthEndpoint("/auth/forgot-password", "forgot-password");
    private static final AuthEndpoint RESEND_CONFIRMATION = new AuthEndpoint("/auth/resend-confirmation", "resend-confirmation");

    private final AuthAbuseProtectionProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, WindowCounter> attempts = new ConcurrentHashMap<>();

    public AuthAbuseProtectionFilter(AuthAbuseProtectionProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    AuthAbuseProtectionFilter(AuthAbuseProtectionProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
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
            : new ContentCachingRequestWrapper(request);
        AuthEndpoint endpoint = endpointFor(wrappedRequest).orElseThrow();
        String ipAddress = clientIp(wrappedRequest);
        String rateKey = endpoint.name() + ":" + ipAddress;

        if (!allow(rateKey, limitFor(endpoint))) {
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
        if (endpoint == LOGIN) {
            return properties.login();
        }
        if (endpoint == RESEND_CONFIRMATION) {
            return properties.resendConfirmation();
        }
        return properties.forgotPassword();
    }

    private boolean allow(String key, AuthAbuseProtectionProperties.Limit limit) {
        if (limit.maxAttempts() <= 0) {
            return false;
        }

        Instant now = clock.instant();
        WindowCounter counter = attempts.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.expiresAt())) {
                return new WindowCounter(1, now.plus(limit.window()));
            }
            return new WindowCounter(current.count() + 1, current.expiresAt());
        });
        return counter.count() <= limit.maxAttempts();
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String safeHeader(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replaceAll("[\\r\\n\\t]", " ").trim();
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
