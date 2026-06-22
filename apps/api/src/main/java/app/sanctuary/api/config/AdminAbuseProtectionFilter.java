package app.sanctuary.api.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class AdminAbuseProtectionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AdminAbuseProtectionFilter.class);
    private static final String ADMIN_PATH_PREFIX = "/admin/";

    private final AdminAbuseProtectionProperties properties;
    private final Clock clock;
    private final Map<String, WindowCounter> attempts = new ConcurrentHashMap<>();

    public AdminAbuseProtectionFilter(AdminAbuseProtectionProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AdminAbuseProtectionFilter(AdminAbuseProtectionProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String ipAddress = clientIp(request);
        String rateKey = ipAddress;

        if (!allow(rateKey, properties.requestLimit())) {
            log.warn(
                "Admin API rate limit exceeded method={} path={} ip={} forwardedFor={} userAgent={}",
                request.getMethod(),
                request.getRequestURI(),
                ipAddress,
                safeHeader(request, "X-Forwarded-For"),
                safeHeader(request, "User-Agent")
            );
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Too many admin requests.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean allow(String key, AdminAbuseProtectionProperties.Limit limit) {
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

    private record WindowCounter(int count, Instant expiresAt) {
    }
}
