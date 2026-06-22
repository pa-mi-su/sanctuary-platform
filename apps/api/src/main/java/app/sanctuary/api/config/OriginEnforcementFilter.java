package app.sanctuary.api.config;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class OriginEnforcementFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OriginEnforcementFilter.class);
    private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final WebProperties webProperties;

    public OriginEnforcementFilter(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !UNSAFE_METHODS.contains(request.getMethod().toUpperCase());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!allowedOrigins().contains(origin.trim())) {
            log.warn(
                "Rejected unsafe request from untrusted origin method={} path={} origin={} ip={} userAgent={}",
                request.getMethod(),
                request.getRequestURI(),
                safeValue(origin),
                clientIp(request),
                safeHeader(request, "User-Agent")
            );
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Origin is not allowed.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Set<String> allowedOrigins() {
        if (webProperties.allowedOrigins().isEmpty()) {
            return Set.of(WebConfig.DEFAULT_ALLOWED_ORIGINS);
        }
        return Set.copyOf(webProperties.allowedOrigins());
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
        return safeValue(request.getHeader(name));
    }

    private String safeValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.replaceAll("[\\r\\n\\t]", " ").trim();
    }
}
