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
    void limitsAuthRequestsByForwardedIp() throws Exception {
        AuthAbuseProtectionProperties properties = new AuthAbuseProtectionProperties();
        properties.setRegister(new AuthAbuseProtectionProperties.Limit(1, Duration.ofHours(1)));
        AuthAbuseProtectionFilter filter = new AuthAbuseProtectionFilter(properties, objectMapper);

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(registerRequest(), firstResponse, new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(registerRequest(), secondResponse, new MockFilterChain());

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
