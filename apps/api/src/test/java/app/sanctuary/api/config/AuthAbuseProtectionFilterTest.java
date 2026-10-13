package app.sanctuary.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthAbuseProtectionFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void forgedForwardedHeadersCannotBypassRateLimit() throws Exception {
        AuthAbuseProtectionProperties properties = new AuthAbuseProtectionProperties();
        properties.setRegister(new AuthAbuseProtectionProperties.Limit(1, Duration.ofHours(1)));
        AuthAbuseProtectionFilter filter = new AuthAbuseProtectionFilter(properties, objectMapper);

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(registerRequest(), firstResponse, new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        MockHttpServletRequest forged = registerRequest();
        forged.removeHeader("X-Forwarded-For");
        forged.addHeader("X-Forwarded-For", "198.51.100.99");
        forged.addHeader("X-Real-IP", "198.51.100.100");
        filter.doFilter(forged, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentAsString()).contains("Too many attempts");
    }

    @Test
    void ignoresNonAuthRoutes() throws Exception {
        AuthAbuseProtectionProperties properties = new AuthAbuseProtectionProperties();
        properties.setRegister(new AuthAbuseProtectionProperties.Limit(0, Duration.ofHours(1)));
        AuthAbuseProtectionFilter filter = new AuthAbuseProtectionFilter(properties, objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void clientsBehindTrustedAlbHaveIndependentLimitsAndForgedPrefixesDoNotHelp() throws Exception {
        var properties = new AuthAbuseProtectionProperties();
        properties.setTrustedProxyCidrs(java.util.Set.of("172.30.0.0/24"));
        properties.setLogin(new AuthAbuseProtectionProperties.Limit(1, Duration.ofMinutes(15)));
        var filter = new AuthAbuseProtectionFilter(properties, objectMapper);
        assertThat(attempt(filter, "/auth/login", "172.30.0.20", "192.0.2.99, 203.0.113.1")).isEqualTo(200);
        assertThat(attempt(filter, "/auth/login", "172.30.0.20", "203.0.113.2")).isEqualTo(200);
        assertThat(attempt(filter, "/auth/login", "172.30.0.21", "192.0.2.100, 203.0.113.1")).isEqualTo(429);
    }

    @Test
    void adminLoginAndMfaCannotConsumeConsumerLoginAllowance() throws Exception {
        var properties = new AuthAbuseProtectionProperties();
        properties.setLogin(new AuthAbuseProtectionProperties.Limit(1, Duration.ofMinutes(15)));
        var filter = new AuthAbuseProtectionFilter(properties, objectMapper);
        assertThat(attempt(filter, "/auth/news/login", "203.0.113.1", null)).isEqualTo(200);
        assertThat(attempt(filter, "/auth/news/verify", "203.0.113.1", null)).isEqualTo(429);
        assertThat(attempt(filter, "/auth/login", "203.0.113.1", null)).isEqualTo(200);
        assertThat(attempt(filter, "/auth/login", "203.0.113.1", null)).isEqualTo(429);
    }

    @Test
    void malformedForwardingFallsBackToPeerAndUntrustedPeersCannotSpoof() throws Exception {
        var properties = new AuthAbuseProtectionProperties();
        properties.setTrustedProxyCidrs(java.util.Set.of("172.30.0.0/24"));
        properties.setLogin(new AuthAbuseProtectionProperties.Limit(1, Duration.ofMinutes(15)));
        var filter = new AuthAbuseProtectionFilter(properties, objectMapper);
        assertThat(attempt(filter, "/auth/login", "172.30.0.20", "evil.example.org")).isEqualTo(200);
        assertThat(attempt(filter, "/auth/login", "172.30.0.20", "999.1.2.3")).isEqualTo(429);
        assertThat(attempt(filter, "/auth/login", "198.51.100.1", "203.0.113.1")).isEqualTo(200);
        assertThat(attempt(filter, "/auth/login", "198.51.100.1", "203.0.113.2")).isEqualTo(429);
    }

    @Test
    void ipv6AliasesCannotBypassLimit() throws Exception {
        var properties = new AuthAbuseProtectionProperties();
        properties.setTrustedProxyCidrs(java.util.Set.of("172.30.0.0/24"));
        properties.setLogin(new AuthAbuseProtectionProperties.Limit(1, Duration.ofMinutes(15)));
        var filter = new AuthAbuseProtectionFilter(properties, objectMapper);
        assertThat(attempt(filter, "/auth/login", "172.30.0.20", "2001:db8::1")).isEqualTo(200);
        assertThat(attempt(filter, "/auth/login", "172.30.0.20", "2001:db8:0:0:0:0:0:1")).isEqualTo(429);
    }

    private int attempt(AuthAbuseProtectionFilter filter, String path, String peer, String forwarded) throws Exception {
        var request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(peer);
        if (forwarded != null) request.addHeader("X-Forwarded-For", forwarded);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    private MockHttpServletRequest registerRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/register");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        request.setContentType("application/json");
        request.setContent("""
            {"email":"john.doe@example.com","password":"Password1234","firstName":"John","lastName":"Doe"}
            """.getBytes());
        return request;
    }
}
